package com.clothingretail.order.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateOrderRequest(
        @NotBlank(message = "must not be blank") String idempotencyKey,
        @NotNull(message = "must not be null") Long shippingAddressId,
        String contactPhone) {}
