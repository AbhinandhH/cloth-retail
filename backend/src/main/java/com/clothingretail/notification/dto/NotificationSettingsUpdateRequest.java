package com.clothingretail.notification.dto;

import jakarta.validation.constraints.NotNull;

public record NotificationSettingsUpdateRequest(
        @NotNull(message = "must not be null") Boolean emailVerificationEnabled,
        @NotNull(message = "must not be null") Boolean mobileVerificationEnabled) {}
