package com.clothingretail.inventory.dto;

import com.clothingretail.inventory.DamageReason;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record DamageRequest(
        @NotNull(message = "must not be null") @Positive(message = "must be positive") Integer quantity,
        @NotNull(message = "must not be null") DamageReason reason,
        String notes) {}
