package com.clothingretail.masterdata.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CategoryAdminRequest(
        @NotBlank(message = "must not be blank") String name,
        @NotBlank(message = "must not be blank") String slug,
        Integer displayOrder,
        @NotNull(message = "must not be null") Boolean active) {}
