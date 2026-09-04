package com.clothingretail.cart.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record AddCartItemRequest(
        @NotNull(message = "must not be null") Long productVariantId,
        @NotNull(message = "must not be null") @Min(value = 1, message = "must be at least 1") Integer quantity) {}
