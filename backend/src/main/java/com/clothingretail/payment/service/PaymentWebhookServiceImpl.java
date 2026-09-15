package com.clothingretail.payment.service;

import com.clothingretail.cart.repository.CartItemRepository;
import com.clothingretail.common.BadRequestException;
import com.clothingretail.common.NotFoundException;
import com.clothingretail.inventory.InventoryTransaction;
import com.clothingretail.inventory.InventoryTransactionType;
import com.clothingretail.inventory.repository.InventoryTransactionRepository;
import com.clothingretail.order.Order;
import com.clothingretail.order.OrderItem;
import com.clothingretail.order.repository.OrderRepository;
import com.clothingretail.order.OrderStatus;
import com.clothingretail.order.service.OrderStatusHistoryService;
import com.clothingretail.payment.Payment;
import com.clothingretail.payment.PaymentStatus;
import com.clothingretail.payment.repository.PaymentRepository;
import com.clothingretail.product.ProductVariant;
import com.clothingretail.product.repository.ProductVariantRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Optional;
import lombok.extern.log4j.Log4j2;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Log4j2
public class PaymentWebhookServiceImpl implements PaymentWebhookService {

    private final PaymentGateway paymentGateway;
    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final ProductVariantRepository productVariantRepository;
    private final InventoryTransactionRepository inventoryTransactionRepository;
    private final OrderStatusHistoryService orderStatusHistoryService;
    private final CartItemRepository cartItemRepository;
    // See MockPaymentGateway for why this is a plain unmanaged instance rather than an injected
    // bean: Spring's auto-configured JSON binder here is Jackson 3 (tools.jackson.*), not this
    // classic com.fasterxml.jackson ObjectMapper.
    private final ObjectMapper objectMapper = new ObjectMapper();

    public PaymentWebhookServiceImpl(
            PaymentGateway paymentGateway,
            PaymentRepository paymentRepository,
            OrderRepository orderRepository,
            ProductVariantRepository productVariantRepository,
            InventoryTransactionRepository inventoryTransactionRepository,
            OrderStatusHistoryService orderStatusHistoryService,
            CartItemRepository cartItemRepository) {
        this.paymentGateway = paymentGateway;
        this.paymentRepository = paymentRepository;
        this.orderRepository = orderRepository;
        this.productVariantRepository = productVariantRepository;
        this.inventoryTransactionRepository = inventoryTransactionRepository;
        this.orderStatusHistoryService = orderStatusHistoryService;
        this.cartItemRepository = cartItemRepository;
    }

    @Override
    @Transactional
    public WebhookResult handleWebhook(String payload, String signature) {
        log.info("[1811] Webhook received payloadLength={}, signaturePresent={}", payload == null ? 0 : payload.length(), signature != null);
        if (!paymentGateway.verifySignature(payload, signature)) {
            // Reuses the existing auth-failure -> 401 ApiError mapping (GlobalExceptionHandler) -
            // an invalid signature is exactly an authentication failure for this endpoint, which
            // trusts the signature instead of a JWT.
            log.error("[1812] Webhook rejected - invalid signature");
            throw new BadCredentialsException("Invalid webhook signature");
        }

        WebhookPayload parsed = parse(payload);
        log.info("[1814] Webhook parsed eventId={}, gatewayReference={}, outcome={}", parsed.eventId(), parsed.gatewayReference(), parsed.outcome());
        return applyOutcome(parsed.eventId(), parsed.gatewayReference(), parsed.outcome(), null);
    }

    /**
     * The gateway-agnostic core every real webhook path funnels into, once that path has already
     * verified its own signature and parsed its own payload shape into these plain arguments - see
     * {@link #handleWebhook} above (MockPaymentGateway's custom JSON) and RazorpayWebhookController
     * (Razorpay's real webhook JSON). {@code eventId} only needs to be stable and unique per
     * logical event *for this gateway* - MockPaymentGateway mints a random one per simulated call;
     * Razorpay has no single dedicated event-id field in its webhook body, so its caller derives one
     * from the event type + payment id instead, which is equally sufficient for this method's
     * idempotency check.
     */
    @Override
    @Transactional
    public WebhookResult applyOutcome(String eventId, String gatewayReference, PaymentOutcome outcome, String gatewayPaymentId) {
        // Fast idempotency path: this exact event id was already applied - a pure no-op replay.
        Optional<Payment> alreadyByEvent = paymentRepository.findByWebhookEventId(eventId);
        if (alreadyByEvent.isPresent()) {
            log.info(
                    "[1815] Webhook idempotent replay - eventId={} already applied, paymentId={}, status={}",
                    eventId, alreadyByEvent.get().getId(), alreadyByEvent.get().getStatus());
            return toResult(alreadyByEvent.get());
        }

        Payment payment = paymentRepository.findByGatewayReference(gatewayReference)
                .orElseThrow(() -> {
                    log.error("[1816] Webhook failed - unknown payment reference={}", gatewayReference);
                    return new NotFoundException("Unknown payment reference: " + gatewayReference);
                });

        if (payment.getStatus() != PaymentStatus.PENDING) {
            // Already resolved by an earlier event (possibly a different event id reporting the
            // same outcome) - no-op, don't reprocess.
            log.info("[1817] Webhook no-op - payment {} already resolved, status={}", payment.getId(), payment.getStatus());
            return toResult(payment);
        }

        PaymentStatus newStatus = outcome == PaymentOutcome.SUCCESS ? PaymentStatus.SUCCESS : PaymentStatus.FAILED;
        // Safe to read before markProcessed's clearing bulk update below: .getId() on a lazy
        // proxy never needs to hit the DB or the persistence context, it's already known from
        // the FK column used to build the proxy in the first place.
        Long orderId = payment.getOrder().getId();
        log.info(
                "[1818] Webhook resolving payment paymentId={}, orderId={}, previousStatus=PENDING, newStatus={}",
                payment.getId(), orderId, newStatus);

        int claimed;
        try {
            // Atomic conditional UPDATE: only succeeds while status is still PENDING, so two
            // concurrent deliveries for this payment can't both apply their stock side effects -
            // exactly the same "single conditional UPDATE, no application lock" pattern used for
            // stock reservation. clearAutomatically=true on this query means every entity
            // reference obtained BEFORE this call (including `payment`) is now detached - only
            // re-fetch by id from here on, never reuse those references for anything but their id.
            claimed = paymentRepository.markProcessed(payment.getId(), newStatus, eventId, PaymentStatus.PENDING, gatewayPaymentId);
            log.info("[1819] Atomic payment status claim result paymentId={}, claimed={}, newStatus={}", payment.getId(), claimed, newStatus);
        } catch (DataIntegrityViolationException raceOnEventId) {
            // A concurrent delivery carrying the exact same event id won the unique-constraint
            // race - re-fetch by event id and report its outcome instead of erroring.
            log.error("[1820] Webhook lost eventId unique-constraint race - eventId={}, paymentId={}", eventId, payment.getId());
            Payment resolved = paymentRepository.findByWebhookEventId(eventId).orElseThrow(() -> raceOnEventId);
            return toResult(resolved);
        }

        if (claimed == 0) {
            // Lost the race to a concurrent webhook delivery that resolved this payment first.
            log.info("[1821] Webhook lost concurrent status-claim race paymentId={}", payment.getId());
            Payment resolved = paymentRepository.findById(payment.getId()).orElseThrow();
            return toResult(resolved);
        }

        Order order = orderRepository.findById(orderId).orElseThrow();
        OrderStatus previousStatus = order.getStatus();
        OrderStatus newOrderStatus;
        if (newStatus == PaymentStatus.SUCCESS) {
            fulfilReservations(order);
            removeSourceCartItems(order);
            newOrderStatus = OrderStatus.CONFIRMED;
        } else {
            releaseReservations(order);
            newOrderStatus = OrderStatus.PAYMENT_FAILED;
        }
        order.setStatus(newOrderStatus);
        orderRepository.save(order);
        orderStatusHistoryService.record(order, previousStatus, newOrderStatus, null, null);
        log.info("[1822] Order status transition from webhook orderId={}, previousStatus={}, newStatus={}", order.getId(), previousStatus, newOrderStatus);

        Payment resolved = paymentRepository.findById(payment.getId()).orElseThrow();
        log.info(
                "[1823] Webhook processed orderId={}, paymentId={}, paymentStatus={}, orderStatus={}",
                resolved.getOrder().getId(), resolved.getId(), resolved.getStatus(), resolved.getOrder().getStatus());
        return toResult(resolved);
    }

    /** Fulfils every line item's reservation: stock actually decrements now, and a SALE_OUT InventoryTransaction is written - same audit pattern as PurchaseService/StockService. */
    private void fulfilReservations(Order order) {
        log.info("[1824] Fulfilling reservations orderId={}, itemCount={}", order.getId(), order.getItems().size());
        for (OrderItem item : order.getItems()) {
            ProductVariant variant = item.getProductVariant();
            if (variant == null) {
                // The variant was deleted after the order was placed - nothing left to decrement.
                log.info("[1825] Skipping fulfillment - variant deleted orderId={}", order.getId());
                continue;
            }
            int previousStock = productVariantRepository.getStockQuantity(variant.getId());
            log.info(
                    "[1826] Fulfilling item variantId={}, orderId={}, quantity={}, previousStock={}",
                    variant.getId(), order.getId(), item.getQuantity(), previousStock);
            int affected = productVariantRepository.decrementStockOnSale(variant.getId(), item.getQuantity());
            if (affected == 0) {
                // Should be unreachable: this exact quantity was already reserved for this exact
                // order item at order-creation time, and nothing else releases someone else's
                // reservation. Fail loudly rather than silently lose an inventory transaction.
                log.error("[1827] Failed to fulfil reservation variantId={}, orderId={}, quantity={}", variant.getId(), order.getId(), item.getQuantity());
                throw new IllegalStateException(
                        "Failed to fulfil reservation for variant " + variant.getId() + " on order " + order.getId());
            }
            int newStock = productVariantRepository.getStockQuantity(variant.getId());
            log.info("[1828] Stock decremented on sale variantId={}, previousStock={}, newStock={}", variant.getId(), previousStock, newStock);

            InventoryTransaction transaction = new InventoryTransaction();
            transaction.setProductVariant(variant);
            transaction.setType(InventoryTransactionType.SALE_OUT);
            transaction.setQuantity(item.getQuantity());
            transaction.setPreviousQuantity(previousStock);
            transaction.setNewQuantity(newStock);
            transaction.setReferenceType("ORDER");
            transaction.setReferenceId(order.getId());
            transaction.setReason("Sale - Order " + order.getOrderNumber());
            inventoryTransactionRepository.save(transaction);
            log.info(
                    "[1829] Sale transaction recorded variantId={}, orderId={}, quantity={}, previousStock={}, newStock={}",
                    variant.getId(), order.getId(), item.getQuantity(), previousStock, newStock);
        }
    }

    /**
     * Removes each line's originating cart_items row now that the order is a confirmed sale - see
     * {@code OrderItem.sourceCartItemId}'s own doc comment for why this only happens here (on
     * payment success), not at order-creation time: a PENDING_PAYMENT order isn't a guaranteed
     * sale yet, so the cart line has to survive a failed/expired payment untouched, and a
     * Buy-Now-scoped order must never disturb the rest of the cart. A missing row (the customer
     * already removed it manually, or it was already cleaned up by a re-delivered webhook) is a
     * silent no-op, not an error.
     */
    private void removeSourceCartItems(Order order) {
        for (OrderItem item : order.getItems()) {
            Long cartItemId = item.getSourceCartItemId();
            if (cartItemId == null) {
                continue;
            }
            cartItemRepository.findById(cartItemId).ifPresentOrElse(
                    cartItemRepository::delete,
                    () -> log.info("[1834] Source cart item {} already gone for orderId={} - nothing to remove", cartItemId, order.getId()));
        }
        log.info("[1833] Removed source cart item(s) for orderId={} after payment success", order.getId());
    }

    /** Payment failed: nothing was ever decremented, so only the reservation needs releasing - no InventoryTransaction. */
    private void releaseReservations(Order order) {
        log.info("[1830] Releasing reservations orderId={}, itemCount={}", order.getId(), order.getItems().size());
        for (OrderItem item : order.getItems()) {
            ProductVariant variant = item.getProductVariant();
            if (variant == null) {
                log.info("[1831] Skipping release - variant deleted orderId={}", order.getId());
                continue;
            }
            productVariantRepository.releaseReservation(variant.getId(), item.getQuantity());
            log.info("[1832] Reservation released variantId={}, orderId={}, quantity={}", variant.getId(), order.getId(), item.getQuantity());
        }
    }

    private WebhookPayload parse(String payload) {
        try {
            return objectMapper.readValue(payload, WebhookPayload.class);
        } catch (Exception ex) {
            log.error("[1813] Malformed webhook payload - failed to parse", ex);
            throw new BadRequestException("Malformed webhook payload");
        }
    }

    private WebhookResult toResult(Payment payment) {
        return new WebhookResult(payment.getOrder().getId(), payment.getOrder().getStatus(), payment.getStatus());
    }
}
