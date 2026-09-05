package com.clothingretail.order.dto;

import com.clothingretail.payment.PaymentStatus;
import java.math.BigDecimal;
import java.time.Instant;

/** {@code failureReason} is always null today - nothing in this codebase records why a gateway declined a payment yet; kept in the shape for forward-compatibility. */
public record AdminPaymentResponse(
        Long id,
        PaymentStatus status,
        String method,
        BigDecimal amount,
        String gatewayReference,
        String failureReason,
        Instant createdAt) {}
