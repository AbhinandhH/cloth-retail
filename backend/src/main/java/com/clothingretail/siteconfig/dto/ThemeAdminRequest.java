package com.clothingretail.siteconfig.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ThemeAdminRequest(
        @NotBlank(message = "must not be blank") String name,
        @NotBlank(message = "must not be blank") @Pattern(regexp = "^#[0-9a-fA-F]{6}$", message = "must be a hex color, e.g. #111827") String primaryColor,
        @NotBlank(message = "must not be blank") @Pattern(regexp = "^#[0-9a-fA-F]{6}$", message = "must be a hex color, e.g. #111827") String secondaryColor,
        @Pattern(regexp = "^#[0-9a-fA-F]{6}$", message = "must be a hex color, e.g. #111827") String accentColor,
        @NotBlank(message = "must not be blank") @Pattern(regexp = "^#[0-9a-fA-F]{6}$", message = "must be a hex color, e.g. #111827") String backgroundColor,
        @NotBlank(message = "must not be blank") @Pattern(regexp = "^#[0-9a-fA-F]{6}$", message = "must be a hex color, e.g. #111827") String textColor,
        Integer displayOrder,
        Boolean richAmbient,
        @Pattern(regexp = "^(SIGNATURE|STUDIO|ELAN)$", message = "must be SIGNATURE, STUDIO, or ELAN") String motif) {}
