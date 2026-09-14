package com.clothingretail.payment;

import com.clothingretail.order.Order;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.Base64;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * Real gateway integration - only registered as a bean when {@code app.payment.provider=razorpay}
 * (see application.yml), and {@code @Primary} so it - not {@link MockPaymentGateway} - is what
 * anything injecting the {@link PaymentGateway} interface gets in that mode. MockPaymentGateway
 * itself is never removed or made conditional: {@code PaymentService}'s dev-only simulate endpoint
 * depends on its concrete type directly and keeps working in either mode, exactly as before this
 * gateway existed.
 *
 * <p>{@link #verifySignature} here only covers Razorpay's <em>webhook</em> signature scheme (HMAC-
 * SHA256 of the raw request body, keyed by the separate webhook secret) - see
 * RazorpayWebhookController. Razorpay's other signature scheme (verifying a Checkout success
 * callback client-side) is intentionally not implemented: this app treats the server-to-server
 * webhook as the sole source of truth for payment state (see PaymentWebhookService's own doc
 * comment), so a client-side callback is only ever used as a UX redirect hint, never something
 * that itself marks an order paid - trusting it for that is exactly the "most common cause of
 * fraudulent orders" Razorpay's own docs warn about, and this design avoids that path entirely
 * rather than needing to defend it.
 */
@Log4j2
@Component
@Primary
@ConditionalOnProperty(prefix = "app.payment", name = "provider", havingValue = "razorpay")
public class RazorpayPaymentGateway implements PaymentGateway {

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final URI ORDERS_URI = URI.create("https://api.razorpay.com/v1/orders");
    private static final Duration HTTP_TIMEOUT = Duration.ofSeconds(10);

    private final String keyId;
    private final String keySecret;
    private final String webhookSecret;
    private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(HTTP_TIMEOUT).build();
    // Same story as MockPaymentGateway/EmailSenderImpl's own ObjectMapper field: this Spring Boot
    // version's auto-configured JSON binder is Jackson 3 (tools.jackson.*), not this classic
    // com.fasterxml.jackson one, so there's no Spring-managed bean of this type to inject.
    private final ObjectMapper objectMapper = new ObjectMapper();

    public RazorpayPaymentGateway(
            @Value("${razorpay.key-id}") String keyId,
            @Value("${razorpay.key-secret}") String keySecret,
            @Value("${razorpay.webhook-secret:}") String webhookSecret) {
        if (keyId == null || keyId.isBlank() || keySecret == null || keySecret.isBlank()) {
            // Fails fast at startup (this bean only exists when app.payment.provider=razorpay),
            // rather than the first time a customer tries to pay - a misconfiguration here should
            // block deployment, not surface as a broken checkout.
            throw new IllegalStateException(
                    "app.payment.provider is razorpay but RAZORPAY_KEY_ID/RAZORPAY_KEY_SECRET are not set");
        }
        if (webhookSecret == null || webhookSecret.isBlank()) {
            log.error("[1870] RAZORPAY_WEBHOOK_SECRET is not set - the /razorpay/webhook endpoint will reject every delivery until it is");
        }
        this.keyId = keyId;
        this.keySecret = keySecret;
        this.webhookSecret = webhookSecret;
    }

    @Override
    public PaymentInitiation initiate(Order order) {
        long amountPaise = toPaise(order.getTotalAmount());
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("amount", amountPaise);
        body.put("currency", "INR");
        body.put("receipt", order.getOrderNumber());
        try {
            HttpRequest request = HttpRequest.newBuilder(ORDERS_URI)
                    .timeout(HTTP_TIMEOUT)
                    .header("Authorization", basicAuth())
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() / 100 != 2) {
                log.error("[1871] Razorpay order creation failed orderId={} status={} body={}", order.getId(), response.statusCode(), response.body());
                throw new IllegalStateException("Razorpay order creation failed: " + extractError(response.body()));
            }
            String razorpayOrderId = objectMapper.readTree(response.body()).get("id").asText();
            log.info("[1872] Razorpay order created orderId={} razorpayOrderId={} amountPaise={}", order.getId(), razorpayOrderId, amountPaise);
            return new PaymentInitiation(razorpayOrderId);
        } catch (Exception ex) {
            log.error("[1873] Razorpay order creation errored orderId={} error={}", order.getId(), ex.getMessage());
            throw new IllegalStateException("Unable to reach Razorpay", ex);
        }
    }

    @Override
    public RefundInitiation refund(Payment payment, BigDecimal amount) {
        String razorpayPaymentId = payment.getGatewayPaymentId();
        if (razorpayPaymentId == null || razorpayPaymentId.isBlank()) {
            // Shouldn't happen: refunds are only ever issued against a SUCCESS payment (see
            // AdminOrderService.refund), and this field is always set by the webhook that first
            // moved a payment to SUCCESS - see PaymentWebhookService.applyOutcome. Fail loudly
            // rather than send a refund request Razorpay can't act on.
            throw new IllegalStateException("Payment " + payment.getId() + " has no Razorpay payment id to refund against");
        }
        URI uri = URI.create("https://api.razorpay.com/v1/payments/" + razorpayPaymentId + "/refund");
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("amount", toPaise(amount));
        try {
            HttpRequest request = HttpRequest.newBuilder(uri)
                    .timeout(HTTP_TIMEOUT)
                    .header("Authorization", basicAuth())
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() / 100 != 2) {
                log.error("[1874] Razorpay refund failed paymentId={} razorpayPaymentId={} status={} body={}",
                        payment.getId(), razorpayPaymentId, response.statusCode(), response.body());
                throw new IllegalStateException("Razorpay refund failed: " + extractError(response.body()));
            }
            String refundId = objectMapper.readTree(response.body()).get("id").asText();
            log.info("[1875] Razorpay refund created paymentId={} razorpayPaymentId={} refundId={} amount={}", payment.getId(), razorpayPaymentId, refundId, amount);
            return new RefundInitiation(refundId);
        } catch (Exception ex) {
            log.error("[1876] Razorpay refund errored paymentId={} razorpayPaymentId={} error={}", payment.getId(), razorpayPaymentId, ex.getMessage());
            throw new IllegalStateException("Unable to reach Razorpay", ex);
        }
    }

    /** Verifies a webhook delivery's X-Razorpay-Signature header - HMAC-SHA256 of the raw request body, keyed by the webhook secret (a distinct value from key-secret above, set in Razorpay's dashboard). */
    @Override
    public boolean verifySignature(String payload, String signature) {
        if (payload == null || signature == null || webhookSecret.isBlank()) {
            return false;
        }
        String expected = hmacHex(payload, webhookSecret);
        // Constant-time comparison so response timing can't leak how much of the signature matched.
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8), signature.getBytes(StandardCharsets.UTF_8));
    }

    private String basicAuth() {
        String credentials = keyId + ":" + keySecret;
        return "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
    }

    private long toPaise(BigDecimal amount) {
        return amount.multiply(BigDecimal.valueOf(100)).setScale(0, RoundingMode.HALF_UP).longValueExact();
    }

    private String hmacHex(String payload, String secret) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            byte[] raw = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(raw);
        } catch (NoSuchAlgorithmException | InvalidKeyException ex) {
            throw new IllegalStateException("Unable to compute HMAC signature", ex);
        }
    }

    /** Razorpay's error body is JSON like {"error": {"description": "..."}}; falls back to the raw body if it isn't. */
    private String extractError(String responseBody) {
        try {
            JsonNode error = objectMapper.readTree(responseBody).get("error");
            if (error != null) {
                JsonNode description = error.get("description");
                if (description != null && !description.isNull()) {
                    return description.asText();
                }
            }
        } catch (Exception ignored) {
            // Not JSON, or unexpected shape - fall through to the raw body below.
        }
        return responseBody;
    }
}
