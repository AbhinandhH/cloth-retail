package com.clothingretail.payment;

import com.clothingretail.common.ConflictException;
import com.clothingretail.common.NotFoundException;
import com.clothingretail.customer.CustomerProfile;
import com.clothingretail.customer.CustomerProfileRepository;
import com.clothingretail.order.Order;
import com.clothingretail.order.OrderRepository;
import com.clothingretail.order.OrderStatus;
import com.clothingretail.payment.dto.PaymentInitiateResponse;
import com.clothingretail.payment.dto.SimulatePaymentRequest;
import com.clothingretail.payment.dto.SimulatePaymentResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final CustomerProfileRepository customerProfileRepository;
    private final PaymentGateway paymentGateway;
    private final MockPaymentGateway mockPaymentGateway;
    private final PaymentWebhookService paymentWebhookService;

    public PaymentService(
            PaymentRepository paymentRepository,
            OrderRepository orderRepository,
            CustomerProfileRepository customerProfileRepository,
            PaymentGateway paymentGateway,
            MockPaymentGateway mockPaymentGateway,
            PaymentWebhookService paymentWebhookService) {
        this.paymentRepository = paymentRepository;
        this.orderRepository = orderRepository;
        this.customerProfileRepository = customerProfileRepository;
        this.paymentGateway = paymentGateway;
        this.mockPaymentGateway = mockPaymentGateway;
        this.paymentWebhookService = paymentWebhookService;
    }

    @Transactional
    public PaymentInitiateResponse initiate(Long userId, Long orderId) {
        CustomerProfile profile = resolveProfile(userId);
        Order order = orderRepository.findById(orderId)
                .filter(o -> o.getCustomerProfile().getId().equals(profile.getId()))
                .orElseThrow(() -> new NotFoundException("Order not found: " + orderId));

        if (order.getStatus() != OrderStatus.PENDING_PAYMENT) {
            throw new ConflictException("Order is not awaiting payment (status: " + order.getStatus() + ")");
        }

        PaymentInitiation initiation = paymentGateway.initiate(order);

        Payment payment = new Payment();
        payment.setOrder(order);
        payment.setStatus(PaymentStatus.PENDING);
        payment.setGatewayReference(initiation.gatewayReference());
        payment.setAmount(order.getTotalAmount());
        payment = paymentRepository.save(payment);

        order.setStatus(OrderStatus.PAYMENT_PROCESSING);
        orderRepository.save(order);

        return new PaymentInitiateResponse(payment.getId(), order.getId(), order.getTotalAmount(), payment.getGatewayReference());
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
    @Transactional
    public SimulatePaymentResponse simulate(Long userId, SimulatePaymentRequest request) {
        CustomerProfile profile = resolveProfile(userId);
        Payment payment = paymentRepository.findByGatewayReference(request.gatewayReference())
                .filter(p -> p.getOrder().getCustomerProfile().getId().equals(profile.getId()))
                .orElseThrow(() -> new NotFoundException("Unknown payment reference"));

        PaymentOutcome outcome = PaymentOutcome.valueOf(request.outcome());
        MockPaymentGateway.SignedWebhookPayload signed = mockPaymentGateway.buildSimulatedWebhook(
                payment.getGatewayReference(), payment.getOrder().getId(), payment.getId(), payment.getAmount(), outcome);

        WebhookResult result = paymentWebhookService.handleWebhook(signed.payload(), signed.signature());
        return new SimulatePaymentResponse(result.orderId(), result.orderStatus(), result.paymentStatus());
    }

    private CustomerProfile resolveProfile(Long userId) {
        return customerProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new NotFoundException("Customer profile not found for user: " + userId));
    }
}
