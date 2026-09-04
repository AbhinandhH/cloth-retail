package com.clothingretail.payment.dto;

import jakarta.validation.constraints.NotNull;

public record InitiatePaymentRequest(@NotNull(message = "must not be null") Long orderId) {}
