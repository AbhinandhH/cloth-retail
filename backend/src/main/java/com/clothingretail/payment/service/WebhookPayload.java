package com.clothingretail.payment.service;

import java.math.BigDecimal;

/**
 * The JSON body a gateway webhook call carries. {@code eventId} is the gateway's own identifier
 * for this specific delivery attempt - {@link PaymentWebhookService} keys idempotency off it
 * (payments.webhook_event_id is unique), so a replayed/duplicate delivery of the same event is a
 * guaranteed no-op regardless of how many times it's retried. {@code timestampEpochMillis} is a
 * plain long (not {@code java.time.Instant}) so (de)serialization needs nothing beyond core
 * Jackson databind - no JSR-310 module dependency/registration required.
 */
record WebhookPayload(
        String eventId,
        String gatewayReference,
        Long orderId,
        Long paymentId,
        BigDecimal amount,
        PaymentOutcome outcome,
        long timestampEpochMillis) {}
