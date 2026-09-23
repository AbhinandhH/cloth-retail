package com.clothingretail.subscription.service;

import com.clothingretail.common.ConflictException;
import com.clothingretail.payment.service.PaymentGateway;
import com.clothingretail.payment.service.PaymentInitiation;
import com.clothingretail.subscription.SubscriptionBilling;
import com.clothingretail.subscription.SubscriptionPayment;
import com.clothingretail.subscription.SubscriptionPaymentStatus;
import com.clothingretail.subscription.dto.SubscriptionPayInitiationResponse;
import com.clothingretail.subscription.dto.SubscriptionPaymentRow;
import com.clothingretail.subscription.dto.SubscriptionSettingsRequest;
import com.clothingretail.subscription.dto.SubscriptionStatusResponse;
import com.clothingretail.subscription.repository.SubscriptionBillingRepository;
import com.clothingretail.subscription.repository.SubscriptionPaymentRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@Log4j2
public class SubscriptionBillingServiceImpl implements SubscriptionBillingService {

    private final SubscriptionBillingRepository billingRepository;
    private final SubscriptionPaymentRepository paymentRepository;
    private final PaymentGateway paymentGateway;
    // Blank when provider is "mock" or unset - same reasoning as PaymentController's own field.
    private final String razorpayKeyId;

    public SubscriptionBillingServiceImpl(
            SubscriptionBillingRepository billingRepository,
            SubscriptionPaymentRepository paymentRepository,
            PaymentGateway paymentGateway,
            @Value("${razorpay.key-id:}") String razorpayKeyId) {
        this.billingRepository = billingRepository;
        this.paymentRepository = paymentRepository;
        this.paymentGateway = paymentGateway;
        this.razorpayKeyId = razorpayKeyId;
    }

    @Override
    public SubscriptionStatusResponse getStatus() {
        return toStatus(loadSingleton());
    }

    @Override
    @Transactional
    public SubscriptionStatusResponse updateSettings(SubscriptionSettingsRequest request) {
        log.info("[2200] Updating subscription settings monthlyAmount={} dueDate={}", request.monthlyAmount(), request.dueDate());
        SubscriptionBilling billing = loadSingleton();
        billing.setMonthlyAmount(request.monthlyAmount());
        billing.setDueDate(request.dueDate());
        billing.setPaid(false);
        billing.setPaidAt(null);
        billing.setLastPaymentReference(null);
        SubscriptionBilling saved = billingRepository.save(billing);
        log.info("[2201] Subscription settings updated - new billing cycle is unpaid until the next payment");
        return toStatus(saved);
    }

    @Override
    @Transactional
    public SubscriptionStatusResponse markPaid() {
        log.info("[2202] Manually marking subscription as paid");
        SubscriptionBilling billing = loadSingleton();
        billing.setPaid(true);
        billing.setPaidAt(Instant.now());
        billing.setLastPaymentReference("manual");
        return toStatus(billingRepository.save(billing));
    }

    @Override
    @Transactional
    public SubscriptionPayInitiationResponse initiatePayment() {
        SubscriptionBilling billing = loadSingleton();
        if (billing.getMonthlyAmount() == null || billing.getDueDate() == null) {
            log.error("[2203] Cannot initiate subscription payment - billing has not been configured yet");
            throw new ConflictException("Billing has not been configured yet");
        }
        if (billing.isPaid()) {
            log.error("[2204] Cannot initiate subscription payment - current cycle is already paid");
            throw new ConflictException("The current billing cycle is already paid");
        }

        String receipt = "subscription-" + UUID.randomUUID();
        PaymentInitiation initiation = paymentGateway.initiateForAmount(billing.getMonthlyAmount(), receipt);

        SubscriptionPayment payment = new SubscriptionPayment();
        payment.setAmount(billing.getMonthlyAmount());
        payment.setGatewayOrderId(initiation.gatewayReference());
        payment.setStatus(SubscriptionPaymentStatus.PENDING);
        paymentRepository.save(payment);
        log.info("[2205] Subscription payment initiated gatewayOrderId={} amount={}", initiation.gatewayReference(), billing.getMonthlyAmount());

        return new SubscriptionPayInitiationResponse(initiation.gatewayReference(), razorpayKeyId, billing.getMonthlyAmount());
    }

    @Override
    public List<SubscriptionPaymentRow> listPayments() {
        return paymentRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(p -> new SubscriptionPaymentRow(p.getId(), p.getAmount(), p.getStatus().name(), p.getCreatedAt()))
                .toList();
    }

    @Override
    @Transactional
    public void applyPaymentOutcome(String gatewayOrderId, String gatewayPaymentId) {
        SubscriptionPayment payment = paymentRepository.findByGatewayOrderId(gatewayOrderId).orElse(null);
        if (payment == null) {
            // Not a subscription payment - most likely a customer-order webhook delivered here too
            // (see SubscriptionWebhookController's own doc comment). Not an error.
            log.info("[2206] Subscription webhook - gatewayOrderId={} is not a known subscription payment, ignoring", gatewayOrderId);
            return;
        }
        if (payment.getStatus() != SubscriptionPaymentStatus.PENDING) {
            log.info("[2207] Subscription webhook no-op - payment {} already resolved, status={}", payment.getId(), payment.getStatus());
            return;
        }

        payment.setGatewayPaymentId(gatewayPaymentId);
        payment.setStatus(SubscriptionPaymentStatus.SUCCESS);
        paymentRepository.save(payment);

        SubscriptionBilling billing = loadSingleton();
        billing.setPaid(true);
        billing.setPaidAt(Instant.now());
        billing.setLastPaymentReference(gatewayPaymentId);
        billingRepository.save(billing);
        log.info("[2208] Subscription payment confirmed gatewayOrderId={} gatewayPaymentId={} - site unlocked", gatewayOrderId, gatewayPaymentId);
    }

    private SubscriptionBilling loadSingleton() {
        return billingRepository.findById(SubscriptionBilling.SINGLETON_ID)
                .orElseThrow(() -> new IllegalStateException(
                        "Singleton subscription_billing row (id=1) is missing - this is a startup-time misconfiguration, "
                                + "check that V36__subscription_billing.sql ran"));
    }

    private SubscriptionStatusResponse toStatus(SubscriptionBilling billing) {
        Long daysUntilDue = billing.getDueDate() != null ? ChronoUnit.DAYS.between(LocalDate.now(), billing.getDueDate()) : null;
        return new SubscriptionStatusResponse(
                billing.getMonthlyAmount(), billing.getDueDate(), billing.isPaid(), billing.isLocked(), daysUntilDue);
    }
}
