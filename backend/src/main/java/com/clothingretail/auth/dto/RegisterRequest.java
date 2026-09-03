package com.clothingretail.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "must not be blank") @Size(max = 150) String fullName,
        @NotBlank(message = "must not be blank") @Email(message = "must be a valid email") String email,
        @Size(max = 20) String mobileNumber,
        @NotBlank(message = "must not be blank") @Size(min = 8, max = 100, message = "must be at least 8 characters") String password) {}
