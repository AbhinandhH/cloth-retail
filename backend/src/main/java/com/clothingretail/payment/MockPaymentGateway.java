package com.clothingretail.payment;

import com.clothingretail.order.Order;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Stands in for a real payment gateway. {@link #initiate} is entirely fake (a random reference,
 * no network call). Signature verification, however, is genuine HMAC-SHA256 over the raw webhook
 * payload bytes with a local shared secret - that logic would work unchanged against a real
 * gateway's HMAC-signed webhooks; only the signing side (also done by this class, standing in for
 * the gateway itself) is mocked. {@link #buildSimulatedWebhook} is what the
 * {@code /api/payments/mock/simulate} dev endpoint uses to construct and sign a fake webhook call
 * for a chosen outcome, so it can be fed through the exact same
 * {@link PaymentWebhookService#handleWebhook(String, String)} a real webhook delivery would use.
 */
@Component
public class MockPaymentGateway implements PaymentGateway {

    private static final String HMAC_ALGORITHM = "HmacSHA256";

    // Spring Boot 4's auto-configured JSON binder in this project is Jackson 3
    // (tools.jackson.databind.ObjectMapper) - there's no Spring-managed bean of the classic
    // com.fasterxml.jackson.databind.ObjectMapper type used here (it's only on the classpath
    // transitively via jjwt-jackson), so this is a plain unmanaged instance, same as the test
    // classes in this codebase already do.
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final String secret;

    public MockPaymentGateway(@Value("${app.payment.mock.secret}") String secret) {
        this.secret = secret;
    }

    @Override
    public PaymentInitiation initiate(Order order) {
        return new PaymentInitiation("mock_" + UUID.randomUUID());
    }

    @Override
    public RefundInitiation refund(Payment payment, BigDecimal amount) {
        // Entirely fake, same as initiate() - an immediate simulated success, no network call.
        return new RefundInitiation("mock_refund_" + UUID.randomUUID());
    }

    @Override
    public boolean verifySignature(String payload, String signature) {
        if (payload == null || signature == null) {
            return false;
        }
        String expected = sign(payload);
        // Constant-time comparison so response timing can't leak how much of the signature matched.
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8), signature.getBytes(StandardCharsets.UTF_8));
    }

    public SignedWebhookPayload buildSimulatedWebhook(
            String gatewayReference, Long orderId, Long paymentId, BigDecimal amount, PaymentOutcome outcome) {
        WebhookPayload payload = new WebhookPayload(
                "evt_" + UUID.randomUUID(), gatewayReference, orderId, paymentId, amount, outcome, Instant.now().toEpochMilli());
        String json = writeJson(payload);
        return new SignedWebhookPayload(json, sign(json));
    }

    private String sign(String payload) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            byte[] raw = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(raw);
        } catch (NoSuchAlgorithmException | InvalidKeyException ex) {
            throw new IllegalStateException("Unable to compute HMAC signature", ex);
        }
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Unable to serialize webhook payload", ex);
        }
    }

    public record SignedWebhookPayload(String payload, String signature) {}
}
