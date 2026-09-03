package com.clothingretail.masterdata.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record VendorAdminRequest(
        @NotBlank(message = "must not be blank") String name,
        String contactName,
        @Email(message = "must be a valid email") String contactEmail,
        String contactPhone,
        @NotNull(message = "must not be null") Boolean active) {}
