package com.clothingretail.payment.service;

/**
 * The one and only place a payment webhook (real or simulated - see {@code PaymentServiceImpl#simulate})
 * is actually processed. Kept as its own bean (distinct from {@link PaymentService}) purely so
 * both {@code PaymentController.webhook} and {@code PaymentServiceImpl.simulate} can call the exact
 * same {@code @Transactional} method through Spring's proxy without running into the
 * self-invocation trap (see {@code OrderCreationService}'s javadoc for the same concern).
 */
public interface PaymentWebhookService {

    WebhookResult handleWebhook(String payload, String signature);

    /**
     * The gateway-agnostic core every real webhook path funnels into, once that path has already
     * verified its own signature and parsed its own payload shape into these plain arguments - see
     * {@link #handleWebhook} above (MockPaymentGateway's custom JSON) and RazorpayWebhookController
     * (Razorpay's real webhook JSON).
     */
    WebhookResult applyOutcome(String eventId, String gatewayReference, PaymentOutcome outcome, String gatewayPaymentId);
}
