package com.clothingretail.notification.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record SmtpTestRequest(@NotBlank(message = "must not be blank") @Email(message = "must be a valid email") String toEmail) {}
