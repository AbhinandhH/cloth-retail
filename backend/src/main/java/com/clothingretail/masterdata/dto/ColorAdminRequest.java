package com.clothingretail.masterdata.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record ColorAdminRequest(
        @NotBlank(message = "must not be blank") String name,
        @NotBlank(message = "must not be blank")
                @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "must be a hex color like #RRGGBB")
                String hexCode,
        Integer displayOrder,
        @NotNull(message = "must not be null") Boolean active) {}
