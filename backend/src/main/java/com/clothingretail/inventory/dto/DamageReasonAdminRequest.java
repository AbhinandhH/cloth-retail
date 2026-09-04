package com.clothingretail.inventory.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record DamageReasonAdminRequest(
        @NotBlank(message = "must not be blank") String name,
        String code,
        String description,
        Integer displayOrder,
        @NotNull(message = "must not be null") Boolean active) {}
