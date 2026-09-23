package com.clothingretail.subscription.controller;

import com.clothingretail.payment.service.RazorpayPaymentGateway;
import com.clothingretail.subscription.service.SubscriptionBillingService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.log4j.Log4j2;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * A deliberately separate Razorpay webhook route from {@code RazorpayWebhookController}'s
 * {@code /api/payments/razorpay/webhook} - subscription billing state stays fully decoupled from
 * order fulfilment. Configure a SECOND webhook entry in the Razorpay dashboard (Settings ->
 * Webhooks -> add https://&lt;your-backend&gt;/api/subscription/webhook, subscribed to
 * "payment.captured") alongside the existing order-payment one - Razorpay supports and calls
 * every webhook URL subscribed to an event, so this is a standard, supported setup, not a
 * workaround. If only the original webhook URL is configured, a subscription payment's captured
 * event never reaches this endpoint at all; {@link SubscriptionBillingService#applyPaymentOutcome}
 * would then need to be reached another way (SUPER_ADMIN's manual "Mark as paid" override exists
 * for exactly this kind of gap).
 *
 * <p>Same signature-verification and event-parsing shape as RazorpayWebhookController - see that
 * class for the full reasoning on each step. This one is a no-op (not an error) whenever the
 * order id isn't a known subscription payment, since - if the user configures only one webhook
 * URL for both flows - this same endpoint would otherwise also receive ordinary customer-order
 * captured events it has nothing to do with.
 */
@Log4j2
@RestController
@RequestMapping("/api/subscription")
@ConditionalOnProperty(prefix = "app.payment", name = "provider", havingValue = "razorpay")
public class SubscriptionWebhookController {

    private final RazorpayPaymentGateway razorpayPaymentGateway;
    private final SubscriptionBillingService subscriptionBillingService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public SubscriptionWebhookController(
            RazorpayPaymentGateway razorpayPaymentGateway, SubscriptionBillingService subscriptionBillingService) {
        this.razorpayPaymentGateway = razorpayPaymentGateway;
        this.subscriptionBillingService = subscriptionBillingService;
    }

    @PostMapping("/webhook")
    public ResponseEntity<Void> webhook(@RequestBody String payload, @RequestHeader("X-Razorpay-Signature") String signature) {
        log.info("[2220] Subscription webhook received payloadLength={}", payload == null ? 0 : payload.length());
        if (!razorpayPaymentGateway.verifySignature(payload, signature)) {
            log.error("[2221] Subscription webhook rejected - invalid signature");
            throw new BadCredentialsException("Invalid webhook signature");
        }

        JsonNode root;
        try {
            root = objectMapper.readTree(payload);
        } catch (Exception ex) {
            log.error("[2222] Subscription webhook - malformed payload", ex);
            return ResponseEntity.ok().build();
        }

        String event = textOrNull(root, "event");
        if (!"payment.captured".equals(event)) {
            log.info("[2223] Subscription webhook ignored - unhandled event={}", event);
            return ResponseEntity.ok().build();
        }

        JsonNode paymentEntity = root.path("payload").path("payment").path("entity");
        String razorpayPaymentId = textOrNull(paymentEntity, "id");
        String razorpayOrderId = textOrNull(paymentEntity, "order_id");
        if (razorpayOrderId == null || razorpayPaymentId == null) {
            log.error("[2224] Subscription webhook missing order/payment id event={}", event);
            return ResponseEntity.ok().build();
        }

        subscriptionBillingService.applyPaymentOutcome(razorpayOrderId, razorpayPaymentId);
        return ResponseEntity.ok().build();
    }

    private String textOrNull(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value != null && !value.isNull() ? value.asText() : null;
    }
}
