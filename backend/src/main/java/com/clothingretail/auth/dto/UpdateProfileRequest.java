package com.clothingretail.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** currentPassword is required only when newPassword is supplied - changing your name/email alone needs no password check. */
public record UpdateProfileRequest(
        @NotBlank(message = "must not be blank") @Size(max = 150) String fullName,
        @NotBlank(message = "must not be blank") @Email(message = "must be a valid email") String email,
        String currentPassword,
        @Size(min = 8, max = 100, message = "must be at least 8 characters") String newPassword) {}
