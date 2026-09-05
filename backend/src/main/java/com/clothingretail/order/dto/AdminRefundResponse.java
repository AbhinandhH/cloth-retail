package com.clothingretail.order.dto;

import com.clothingretail.payment.RefundStatus;
import java.math.BigDecimal;
import java.time.Instant;

/** Used both as the order detail's {@code refund} field and as the POST /{id}/refund response body. */
public record AdminRefundResponse(Long id, BigDecimal amount, RefundStatus status, String reference, Instant createdAt) {}
