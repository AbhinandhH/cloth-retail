package com.clothingretail.inventory.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record StockAdjustRequest(
        @NotNull(message = "must not be null") Integer quantityChange,
        @NotBlank(message = "must not be blank") String reason) {}
