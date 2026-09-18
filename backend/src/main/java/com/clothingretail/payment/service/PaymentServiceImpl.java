package com.clothingretail.payment.service;

import com.clothingretail.common.ConflictException;
import com.clothingretail.common.NotFoundException;
import com.clothingretail.customer.CustomerProfile;
import com.clothingretail.customer.repository.CustomerProfileRepository;
import com.clothingretail.order.Order;
import com.clothingretail.order.OrderItem;
import com.clothingretail.order.repository.OrderRepository;
import com.clothingretail.order.OrderStatus;
import com.clothingretail.order.service.OrderStatusHistoryService;
import com.clothingretail.payment.Payment;
import com.clothingretail.payment.PaymentStatus;
import com.clothingretail.payment.dto.PaymentInitiateResponse;
import com.clothingretail.payment.dto.SimulatePaymentRequest;
import com.clothingretail.payment.dto.SimulatePaymentResponse;
import com.clothingretail.payment.repository.PaymentRepository;
import com.clothingretail.product.ProductVariant;
import com.clothingretail.product.repository.ProductVariantRepository;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Log4j2
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final CustomerProfileRepository customerProfileRepository;
    private final PaymentGateway paymentGateway;
    private final MockPaymentGateway mockPaymentGateway;
    private final PaymentWebhookService paymentWebhookService;
    private final OrderStatusHistoryService orderStatusHistoryService;
    private final ProductVariantRepository productVariantRepository;

    public PaymentServiceImpl(
            PaymentRepository paymentRepository,
            OrderRepository orderRepository,
            CustomerProfileRepository customerProfileRepository,
            PaymentGateway paymentGateway,
            MockPaymentGateway mockPaymentGateway,
            PaymentWebhookService paymentWebhookService,
            OrderStatusHistoryService orderStatusHistoryService,
            ProductVariantRepository productVariantRepository) {
        this.paymentRepository = paymentRepository;
        this.orderRepository = orderRepository;
        this.customerProfileRepository = customerProfileRepository;
        this.paymentGateway = paymentGateway;
        this.mockPaymentGateway = mockPaymentGateway;
        this.paymentWebhookService = paymentWebhookService;
        this.orderStatusHistoryService = orderStatusHistoryService;
        this.productVariantRepository = productVariantRepository;
    }

    @Override
    @Transactional
    public PaymentInitiateResponse initiate(Long userId, Long orderId, String paymentMethod) {
        log.info("[1800] Initiating payment userId={}, orderId={}, paymentMethod={}", userId, orderId, paymentMethod);
        CustomerProfile profile = resolveProfile(userId);
        Order order = orderRepository.findById(orderId)
                .filter(o -> o.getCustomerProfile().getId().equals(profile.getId()))
                .orElseThrow(() -> {
                    log.error("[1802] Payment initiation failed - order not found or not owned userId={}, orderId={}", userId, orderId);
                    return new NotFoundException("Order not found: " + orderId);
                });

        if (order.getStatus() != OrderStatus.PENDING_PAYMENT) {
            log.error("[1803] Payment initiation rejected - order {} not awaiting payment, status={}", orderId, order.getStatus());
            throw new ConflictException("Order is not awaiting payment (status: " + order.getStatus() + ")");
        }

        // Atomic conditional UPDATE, not a plain save() later - closes a real race where two
        // concurrent initiate() calls for the same order (two tabs, a slipped double-click) could
        // otherwise both pass the in-memory status check above before either commits. Combined
        // with the reservation loop below, that race could silently double-reserve one order's
        // stock. Only one caller's transition can succeed; the loser fails here, before touching
        // stock, the gateway, or a Payment row.
        int transitioned = orderRepository.transitionStatus(order.getId(), OrderStatus.PENDING_PAYMENT, OrderStatus.PAYMENT_PROCESSING);
        if (transitioned == 0) {
            log.error("[2001] Payment initiation lost a race - order {} was no longer PENDING_PAYMENT by the time of the atomic transition", orderId);
            throw new ConflictException("Order is not awaiting payment");
        }

        if (!order.isStockReserved()) {
            for (OrderItem item : order.getItems()) {
                ProductVariant variant = item.getProductVariant();
                if (variant == null) {
                    log.error("[2002] Deferred reservation failed - order {} item {} has no variant (deleted since the order was drafted)", orderId, item.getId());
                    throw new ConflictException("'" + item.getProductName() + "' (" + item.getSku() + ") is no longer available");
                }
                int affected = productVariantRepository.reserveStock(variant.getId(), item.getQuantity());
                if (affected == 0) {
                    log.error("[2003] Deferred reservation failed for order {} variant {} (sku={}): requested={}, available={}",
                            orderId, variant.getId(), variant.getSku(), item.getQuantity(), variant.getAvailableQuantity());
                    throw new ConflictException(
                            "'" + item.getProductName() + "' (" + item.getSku() + ") is no longer available "
                                    + "in the requested quantity (" + item.getQuantity() + ") - only "
                                    + variant.getAvailableQuantity() + " left");
                }
                log.info("[2004] Deferred reservation succeeded at Pay-click for order {} variant {} (sku={}): quantity={}",
                        orderId, variant.getId(), variant.getSku(), item.getQuantity());
            }
            order.setStockReserved(true);
        }

        PaymentInitiation initiation = paymentGateway.initiate(order);
        log.info("[1804] Gateway initiation succeeded orderId={}, gatewayReference={}", orderId, initiation.gatewayReference());

        Payment payment = new Payment();
        payment.setOrder(order);
        payment.setStatus(PaymentStatus.PENDING);
        payment.setGatewayReference(initiation.gatewayReference());
        payment.setAmount(order.getTotalAmount());
        payment.setPaymentMethod(hasText(paymentMethod) ? paymentMethod : "UPI");
        payment = paymentRepository.save(payment);
        log.info("[1805] Payment created id={}, orderId={}, status={}, amount={}", payment.getId(), orderId, payment.getStatus(), payment.getAmount());

        OrderStatus previousStatus = order.getStatus();
        order.setStatus(OrderStatus.PAYMENT_PROCESSING);
        orderRepository.save(order);
        orderStatusHistoryService.record(order, previousStatus, OrderStatus.PAYMENT_PROCESSING, null, null);
        log.info("[1806] Order status transition orderId={}, previousStatus={}, newStatus={}", order.getId(), previousStatus, order.getStatus());

        return new PaymentInitiateResponse(payment.getId(), order.getId(), order.getTotalAmount(), payment.getGatewayReference());
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    /**
     * Dev-only helper behind the customer's own JWT: looks up the payment (ownership-checked),
     * has {@link MockPaymentGateway} sign a fake webhook payload for the requested outcome, and
     * feeds it through {@link PaymentWebhookService#handleWebhook(String, String)} - the exact
     * same method a real gateway's webhook call would hit. {@code @Transactional} here so the
     * ownership check's lazy loads (payment -&gt; order -&gt; customerProfile) happen inside an
     * open session; the nested call into {@code handleWebhook} (default REQUIRED propagation)
     * simply joins this same transaction rather than opening a second one, which is fine - the
     * ownership check and the processing it gates belong together atomically anyway.
     */
    @Override
    @Transactional
    public SimulatePaymentResponse simulate(Long userId, SimulatePaymentRequest request) {
        log.info("[1807] Simulating payment webhook userId={}, gatewayReference={}, outcome={}", userId, request.gatewayReference(), request.outcome());
        CustomerProfile profile = resolveProfile(userId);
        Payment payment = paymentRepository.findByGatewayReference(request.gatewayReference())
                .filter(p -> p.getOrder().getCustomerProfile().getId().equals(profile.getId()))
                .orElseThrow(() -> {
                    log.error("[1808] Payment simulation failed - unknown/not-owned gatewayReference={}, userId={}", request.gatewayReference(), userId);
                    return new NotFoundException("Unknown payment reference");
                });

        PaymentOutcome outcome = PaymentOutcome.valueOf(request.outcome());
        MockPaymentGateway.SignedWebhookPayload signed = mockPaymentGateway.buildSimulatedWebhook(
                payment.getGatewayReference(), payment.getOrder().getId(), payment.getId(), payment.getAmount(), outcome);
        log.info(
                "[1809] Simulated webhook built gatewayReference={}, orderId={}, paymentId={}, amount={}, outcome={}",
                payment.getGatewayReference(), payment.getOrder().getId(), payment.getId(), payment.getAmount(), outcome);

        WebhookResult result = paymentWebhookService.handleWebhook(signed.payload(), signed.signature());
        log.info("[1810] Payment simulation result orderId={}, orderStatus={}, paymentStatus={}", result.orderId(), result.orderStatus(), result.paymentStatus());
        return new SimulatePaymentResponse(result.orderId(), result.orderStatus(), result.paymentStatus());
    }

    private CustomerProfile resolveProfile(Long userId) {
        return customerProfileRepository.findByUserId(userId)
                .orElseThrow(() -> {
                    log.error("[1801] Payment operation failed - customer profile not found userId={}", userId);
                    return new NotFoundException("Customer profile not found for user: " + userId);
                });
    }
}
