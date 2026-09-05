package com.clothingretail.payment;

import com.clothingretail.common.BadRequestException;
import com.clothingretail.common.NotFoundException;
import com.clothingretail.inventory.InventoryTransaction;
import com.clothingretail.inventory.InventoryTransactionRepository;
import com.clothingretail.inventory.InventoryTransactionType;
import com.clothingretail.order.Order;
import com.clothingretail.order.OrderItem;
import com.clothingretail.order.OrderRepository;
import com.clothingretail.order.OrderStatus;
import com.clothingretail.order.OrderStatusHistoryService;
import com.clothingretail.product.ProductVariant;
import com.clothingretail.product.ProductVariantRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Optional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The one and only place a payment webhook (real or simulated - see {@code PaymentService#simulate})
 * is actually processed. Kept as its own bean (distinct from {@link PaymentService}) purely so
 * both {@code PaymentController.webhook} and {@code PaymentService.simulate} can call the exact
 * same {@code @Transactional} method through Spring's proxy without running into the
 * self-invocation trap (see {@code OrderCreationService}'s javadoc for the same concern).
 */
@Service
public class PaymentWebhookService {

    private final PaymentGateway paymentGateway;
    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final ProductVariantRepository productVariantRepository;
    private final InventoryTransactionRepository inventoryTransactionRepository;
    private final OrderStatusHistoryService orderStatusHistoryService;
    // See MockPaymentGateway for why this is a plain unmanaged instance rather than an injected
    // bean: Spring's auto-configured JSON binder here is Jackson 3 (tools.jackson.*), not this
    // classic com.fasterxml.jackson ObjectMapper.
    private final ObjectMapper objectMapper = new ObjectMapper();

    public PaymentWebhookService(
            PaymentGateway paymentGateway,
            PaymentRepository paymentRepository,
            OrderRepository orderRepository,
            ProductVariantRepository productVariantRepository,
            InventoryTransactionRepository inventoryTransactionRepository,
            OrderStatusHistoryService orderStatusHistoryService) {
        this.paymentGateway = paymentGateway;
        this.paymentRepository = paymentRepository;
        this.orderRepository = orderRepository;
        this.productVariantRepository = productVariantRepository;
        this.inventoryTransactionRepository = inventoryTransactionRepository;
        this.orderStatusHistoryService = orderStatusHistoryService;
    }

    @Transactional
    public WebhookResult handleWebhook(String payload, String signature) {
        if (!paymentGateway.verifySignature(payload, signature)) {
            // Reuses the existing auth-failure -> 401 ApiError mapping (GlobalExceptionHandler) -
            // an invalid signature is exactly an authentication failure for this endpoint, which
            // trusts the signature instead of a JWT.
            throw new BadCredentialsException("Invalid webhook signature");
        }

        WebhookPayload parsed = parse(payload);

        // Fast idempotency path: this exact event id was already applied - a pure no-op replay.
        Optional<Payment> alreadyByEvent = paymentRepository.findByWebhookEventId(parsed.eventId());
        if (alreadyByEvent.isPresent()) {
            return toResult(alreadyByEvent.get());
        }

        Payment payment = paymentRepository.findByGatewayReference(parsed.gatewayReference())
                .orElseThrow(() -> new NotFoundException("Unknown payment reference: " + parsed.gatewayReference()));

        if (payment.getStatus() != PaymentStatus.PENDING) {
            // Already resolved by an earlier event (possibly a different event id reporting the
            // same outcome) - no-op, don't reprocess.
            return toResult(payment);
        }

        PaymentStatus newStatus = parsed.outcome() == PaymentOutcome.SUCCESS ? PaymentStatus.SUCCESS : PaymentStatus.FAILED;
        // Safe to read before markProcessed's clearing bulk update below: .getId() on a lazy
        // proxy never needs to hit the DB or the persistence context, it's already known from
        // the FK column used to build the proxy in the first place.
        Long orderId = payment.getOrder().getId();

        int claimed;
        try {
            // Atomic conditional UPDATE: only succeeds while status is still PENDING, so two
            // concurrent deliveries for this payment can't both apply their stock side effects -
            // exactly the same "single conditional UPDATE, no application lock" pattern used for
            // stock reservation. clearAutomatically=true on this query means every entity
            // reference obtained BEFORE this call (including `payment`) is now detached - only
            // re-fetch by id from here on, never reuse those references for anything but their id.
            claimed = paymentRepository.markProcessed(payment.getId(), newStatus, parsed.eventId(), PaymentStatus.PENDING);
        } catch (DataIntegrityViolationException raceOnEventId) {
            // A concurrent delivery carrying the exact same event id won the unique-constraint
            // race - re-fetch by event id and report its outcome instead of erroring.
            Payment resolved = paymentRepository.findByWebhookEventId(parsed.eventId()).orElseThrow(() -> raceOnEventId);
            return toResult(resolved);
        }

        if (claimed == 0) {
            // Lost the race to a concurrent webhook delivery that resolved this payment first.
            Payment resolved = paymentRepository.findById(payment.getId()).orElseThrow();
            return toResult(resolved);
        }

        Order order = orderRepository.findById(orderId).orElseThrow();
        OrderStatus previousStatus = order.getStatus();
        OrderStatus newOrderStatus;
        if (newStatus == PaymentStatus.SUCCESS) {
            fulfilReservations(order);
            newOrderStatus = OrderStatus.CONFIRMED;
        } else {
            releaseReservations(order);
            newOrderStatus = OrderStatus.PAYMENT_FAILED;
        }
        order.setStatus(newOrderStatus);
        orderRepository.save(order);
        orderStatusHistoryService.record(order, previousStatus, newOrderStatus, null, null);

        Payment resolved = paymentRepository.findById(payment.getId()).orElseThrow();
        return toResult(resolved);
    }

    /** Fulfils every line item's reservation: stock actually decrements now, and a SALE_OUT InventoryTransaction is written - same audit pattern as PurchaseService/StockService. */
    private void fulfilReservations(Order order) {
        for (OrderItem item : order.getItems()) {
            ProductVariant variant = item.getProductVariant();
            if (variant == null) {
                // The variant was deleted after the order was placed - nothing left to decrement.
                continue;
            }
            int previousStock = productVariantRepository.getStockQuantity(variant.getId());
            int affected = productVariantRepository.decrementStockOnSale(variant.getId(), item.getQuantity());
            if (affected == 0) {
                // Should be unreachable: this exact quantity was already reserved for this exact
                // order item at order-creation time, and nothing else releases someone else's
                // reservation. Fail loudly rather than silently lose an inventory transaction.
                throw new IllegalStateException(
                        "Failed to fulfil reservation for variant " + variant.getId() + " on order " + order.getId());
            }
            int newStock = productVariantRepository.getStockQuantity(variant.getId());

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
        }
    }

    /** Payment failed: nothing was ever decremented, so only the reservation needs releasing - no InventoryTransaction. */
    private void releaseReservations(Order order) {
        for (OrderItem item : order.getItems()) {
            ProductVariant variant = item.getProductVariant();
            if (variant == null) {
                continue;
            }
            productVariantRepository.releaseReservation(variant.getId(), item.getQuantity());
        }
    }

    private WebhookPayload parse(String payload) {
        try {
            return objectMapper.readValue(payload, WebhookPayload.class);
        } catch (Exception ex) {
            throw new BadRequestException("Malformed webhook payload");
        }
    }

    private WebhookResult toResult(Payment payment) {
        return new WebhookResult(payment.getOrder().getId(), payment.getOrder().getStatus(), payment.getStatus());
    }
}
