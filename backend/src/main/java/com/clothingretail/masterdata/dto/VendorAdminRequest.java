package com.clothingretail.masterdata.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record VendorAdminRequest(
        @NotBlank(message = "must not be blank") String name,
        String contactName,
        // Format-only here (blank/null is allowed - the general Masters > Vendors admin
        // screen has always treated this as optional); VendorService additionally enforces
        // case-insensitive uniqueness among non-blank values when one is provided.
        @Email(message = "must be a valid email") String contactEmail,
        // Digits with an optional leading + and optional spaces/hyphens as separators, 7-20
        // characters total - permissive enough for real-world formatting
        // ("+91 98765 43210", "987-654-3210") without accepting obvious garbage. No existing
        // phone-format precedent in this codebase to match; this is the first one.
        @Pattern(regexp = "^\\+?[0-9][0-9\\s-]{6,19}$", message = "must be a valid phone number") String contactPhone,
        @NotNull(message = "must not be null") Boolean active) {}
