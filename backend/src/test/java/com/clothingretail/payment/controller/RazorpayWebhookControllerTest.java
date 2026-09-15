package com.clothingretail.payment.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.clothingretail.payment.service.PaymentOutcome;
import com.clothingretail.payment.service.PaymentWebhookService;
import com.clothingretail.payment.service.RazorpayPaymentGateway;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

/**
 * Plain Mockito unit test (no Spring context needed - the controller is only ever registered
 * behind {@code app.payment.provider=razorpay}, which the test profile doesn't set, so this
 * constructs it directly with stubbed dependencies instead of trying to stand up that profile).
 *
 * Regression coverage for a real, customer-visible bug: a single Razorpay Order can have several
 * payment ATTEMPTS - e.g. a declined card retried with a different one, all within the same
 * Checkout session - each producing its own razorpay_payment_id against the same order_id (our
 * gatewayReference). The webhook handler used to forward payment.failed straight into
 * PaymentWebhookService#applyOutcome as the order's terminal outcome, which locked the Payment
 * row to FAILED the instant the first attempt failed - so a later successful retry's
 * payment.captured was silently discarded as an "already resolved" no-op replay, leaving a
 * genuinely-paid order stuck showing PAYMENT_FAILED. See RazorpayWebhookController's own comment
 * on the payment.failed branch for the fix.
 */
class RazorpayWebhookControllerTest {

    private final RazorpayPaymentGateway gateway = mock(RazorpayPaymentGateway.class);
    private final PaymentWebhookService webhookService = mock(PaymentWebhookService.class);
    private final RazorpayWebhookController controller = new RazorpayWebhookController(gateway, webhookService);

    @Test
    void paymentFailedIsAcknowledgedButNeverResolvesThePayment() {
        when(gateway.verifySignature(anyString(), anyString())).thenReturn(true);
        String payload = """
                {"event":"payment.failed","payload":{"payment":{"entity":{"id":"pay_declined","order_id":"order_abc123"}}}}
                """;

        ResponseEntity<Void> response = controller.webhook(payload, "sig");

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        verify(webhookService, never()).applyOutcome(any(), any(), any(), any());
    }

    @Test
    void paymentCapturedResolvesTheOrderAsSuccess() {
        when(gateway.verifySignature(anyString(), anyString())).thenReturn(true);
        String payload = """
                {"event":"payment.captured","payload":{"payment":{"entity":{"id":"pay_success1","order_id":"order_abc123"}}}}
                """;

        ResponseEntity<Void> response = controller.webhook(payload, "sig");

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        verify(webhookService).applyOutcome("payment.captured:pay_success1", "order_abc123", PaymentOutcome.SUCCESS, "pay_success1");
    }

    @Test
    void declinedAttemptFollowedByASuccessfulRetryStillResolvesAsSuccess() {
        when(gateway.verifySignature(anyString(), anyString())).thenReturn(true);

        // First attempt declined - same Razorpay order, its own payment id.
        String failedPayload = """
                {"event":"payment.failed","payload":{"payment":{"entity":{"id":"pay_declined","order_id":"order_abc123"}}}}
                """;
        controller.webhook(failedPayload, "sig");

        // Retried with a different card, same Razorpay order.
        String capturedPayload = """
                {"event":"payment.captured","payload":{"payment":{"entity":{"id":"pay_retrySuccess","order_id":"order_abc123"}}}}
                """;
        controller.webhook(capturedPayload, "sig");

        verify(webhookService, never()).applyOutcome(any(), any(), eq(PaymentOutcome.FAILURE), any());
        verify(webhookService).applyOutcome("payment.captured:pay_retrySuccess", "order_abc123", PaymentOutcome.SUCCESS, "pay_retrySuccess");
    }

    @Test
    void unhandledEventTypeIsAcknowledgedWithoutResolvingThePayment() {
        when(gateway.verifySignature(anyString(), anyString())).thenReturn(true);
        String payload = """
                {"event":"order.paid","payload":{"payment":{"entity":{"id":"pay_x","order_id":"order_abc123"}}}}
                """;

        ResponseEntity<Void> response = controller.webhook(payload, "sig");

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        verify(webhookService, never()).applyOutcome(any(), any(), any(), any());
    }
}
