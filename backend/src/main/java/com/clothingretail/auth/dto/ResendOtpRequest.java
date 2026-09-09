package com.clothingretail.auth.dto;

import com.clothingretail.auth.OtpChannel;
import jakarta.validation.constraints.NotNull;

public record ResendOtpRequest(
        @NotNull(message = "must not be null") Long registrationId,
        @NotNull(message = "must not be null") OtpChannel channel) {}
