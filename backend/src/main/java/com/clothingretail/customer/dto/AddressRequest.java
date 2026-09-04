package com.clothingretail.customer.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AddressRequest(
        @Size(max = 50, message = "must be at most 50 characters") String label,
        @NotBlank(message = "must not be blank") @Size(max = 255, message = "must be at most 255 characters") String addressLine1,
        @Size(max = 255, message = "must be at most 255 characters") String addressLine2,
        @NotBlank(message = "must not be blank") @Size(max = 100, message = "must be at most 100 characters") String city,
        @NotBlank(message = "must not be blank") @Size(max = 100, message = "must be at most 100 characters") String state,
        @NotBlank(message = "must not be blank") @Size(max = 20, message = "must be at most 20 characters") String postalCode,
        @NotBlank(message = "must not be blank") @Size(max = 100, message = "must be at most 100 characters") String country,
        Boolean isDefault) {}
