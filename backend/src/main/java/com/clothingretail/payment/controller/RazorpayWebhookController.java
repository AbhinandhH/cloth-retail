package com.clothingretail.payment.controller;

import com.clothingretail.payment.service.PaymentOutcome;
import com.clothingretail.payment.service.PaymentWebhookService;
import com.clothingretail.payment.service.RazorpayPaymentGateway;
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
 * Razorpay's real webhook delivery endpoint - a separate route from {@code PaymentController}'s
 * {@code /api/payments/webhook} (MockPaymentGateway's own JSON shape/header, unchanged) since
 * Razorpay's webhook body and signature header are both gateway-specific. Only registered when
 * {@code app.payment.provider=razorpay} (see {@link RazorpayPaymentGateway}) - see SecurityConfig
 * for why this path is in the public allowlist despite carrying no customer JWT: same reasoning
 * as the mock webhook, trust comes entirely from the signature check below.
 *
 * <p>Configure this URL (https://&lt;your-backend&gt;/api/payments/razorpay/webhook) in the
 * Razorpay dashboard under Settings -> Webhooks, subscribed to at least the "payment.captured" and
 * "payment.failed" events - every other event this receives is acknowledged (200) without further
 * processing, per Razorpay's own requirement to ack quickly or be retried.
 */
@Log4j2
@RestController
@RequestMapping("/api/payments/razorpay")
@ConditionalOnProperty(prefix = "app.payment", name = "provider", havingValue = "razorpay")
public class RazorpayWebhookController {

    private final RazorpayPaymentGateway razorpayPaymentGateway;
    private final PaymentWebhookService paymentWebhookService;
    // Same story as RazorpayPaymentGateway's own field - see its doc comment.
    private final ObjectMapper objectMapper = new ObjectMapper();

    public RazorpayWebhookController(RazorpayPaymentGateway razorpayPaymentGateway, PaymentWebhookService paymentWebhookService) {
        this.razorpayPaymentGateway = razorpayPaymentGateway;
        this.paymentWebhookService = paymentWebhookService;
    }

    @PostMapping("/webhook")
    public ResponseEntity<Void> webhook(@RequestBody String payload, @RequestHeader("X-Razorpay-Signature") String signature) {
        log.info("[1877] Razorpay webhook received payloadLength={}", payload == null ? 0 : payload.length());
        if (!razorpayPaymentGateway.verifySignature(payload, signature)) {
            log.error("[1878] Razorpay webhook rejected - invalid signature");
            throw new BadCredentialsException("Invalid webhook signature");
        }

        JsonNode root;
        try {
            root = objectMapper.readTree(payload);
        } catch (Exception ex) {
            log.error("[1879] Razorpay webhook - malformed payload", ex);
            return ResponseEntity.ok().build();
        }

        String event = textOrNull(root, "event");
        JsonNode paymentEntity = root.path("payload").path("payment").path("entity");
        String razorpayPaymentId = textOrNull(paymentEntity, "id");
        String razorpayOrderId = textOrNull(paymentEntity, "order_id");

        if ("payment.failed".equals(event)) {
            // Deliberately NOT forwarded to applyOutcome as a FAILURE outcome. A single Razorpay
            // Order (our gatewayReference) can have several payment ATTEMPTS - e.g. a declined
            // card retried with a different one, all within the same Checkout session - each
            // producing its own razorpay_payment_id against the same order_id. applyOutcome's
            // atomic PENDING -> newStatus claim only lets ONE outcome ever resolve a Payment row:
            // treating this attempt-level failure as the order's terminal outcome would lock the
            // row to FAILED the instant the first attempt fails, and silently discard a later
            // successful retry's payment.captured as a no-op "already resolved" replay - exactly
            // the customer-visible bug this guards against (a genuinely successful payment ending
            // up shown as PAYMENT_FAILED). So a failed attempt is just logged here: the Payment
            // stays PENDING, still eligible to be resolved by a later payment.captured for the
            // same order. If no attempt ever succeeds, OrderReservationCleanupJob's existing
            // reservation-expiry timeout - not this webhook - is what eventually gives up and
            // cancels the order.
            log.info(
                    "[1883] Razorpay webhook - payment attempt failed razorpayOrderId={} razorpayPaymentId={} "
                            + "(Payment left PENDING - may still be resolved by a retried attempt's payment.captured)",
                    razorpayOrderId, razorpayPaymentId);
            return ResponseEntity.ok().build();
        }

        if (!"payment.captured".equals(event)) {
            // Some other event this app doesn't act on (e.g. order.paid, refund.processed) -
            // acknowledge it anyway so Razorpay doesn't keep retrying.
            log.info("[1880] Razorpay webhook ignored - unhandled event={}", event);
            return ResponseEntity.ok().build();
        }

        if (razorpayOrderId == null || razorpayPaymentId == null) {
            log.error("[1881] Razorpay webhook missing order/payment id event={}", event);
            return ResponseEntity.ok().build();
        }

        // Razorpay's webhook body has no single dedicated event-id field - event type + payment id
        // is stable across retried deliveries of the same logical event and unique enough for
        // PaymentWebhookService's idempotency check (see its own doc comment on this parameter).
        String eventId = event + ":" + razorpayPaymentId;
        log.info("[1882] Razorpay webhook parsed event={} razorpayOrderId={} razorpayPaymentId={}", event, razorpayOrderId, razorpayPaymentId);
        paymentWebhookService.applyOutcome(eventId, razorpayOrderId, PaymentOutcome.SUCCESS, razorpayPaymentId);
        return ResponseEntity.ok().build();
    }

    private String textOrNull(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value != null && !value.isNull() ? value.asText() : null;
    }
}
