package com.clothingretail.cart.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record UpdateCartItemRequest(
        @NotNull(message = "must not be null") @Min(value = 1, message = "must be at least 1") Integer quantity) {}
