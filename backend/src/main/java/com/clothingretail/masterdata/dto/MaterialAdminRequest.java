package com.clothingretail.masterdata.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record MaterialAdminRequest(
        @NotBlank(message = "must not be blank") String name,
        Integer displayOrder,
        @NotNull(message = "must not be null") Boolean active) {}
