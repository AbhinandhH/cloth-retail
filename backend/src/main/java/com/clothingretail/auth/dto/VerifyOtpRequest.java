package com.clothingretail.auth.dto;

import com.clothingretail.auth.OtpChannel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record VerifyOtpRequest(
        @NotNull(message = "must not be null") Long registrationId,
        @NotNull(message = "must not be null") OtpChannel channel,
        @NotBlank(message = "must not be blank") String code) {}
