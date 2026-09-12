package com.clothingretail.notification.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record NotificationSettingsUpdateRequest(
        @NotNull(message = "must not be null") Boolean emailVerificationEnabled,
        @NotNull(message = "must not be null") Boolean mobileVerificationEnabled,
        @NotBlank(message = "must not be blank") String emailSubject,
        @NotBlank(message = "must not be blank")
                @Pattern(regexp = ".*\\{code\\}.*", flags = Pattern.Flag.DOTALL, message = "must include {code}")
                String messageTemplate) {}
