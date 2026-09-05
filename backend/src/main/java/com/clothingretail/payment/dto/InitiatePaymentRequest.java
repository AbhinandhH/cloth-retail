package com.clothingretail.payment.dto;

import jakarta.validation.constraints.NotNull;

/** {@code paymentMethod} is optional - PaymentService.initiate defaults it to "UPI" when omitted, so the existing customer frontend (which never sends it) keeps working unchanged. */
public record InitiatePaymentRequest(@NotNull(message = "must not be null") Long orderId, String paymentMethod) {}
