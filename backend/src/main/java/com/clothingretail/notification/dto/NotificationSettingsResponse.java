package com.clothingretail.notification.dto;

public record NotificationSettingsResponse(
        boolean emailVerificationEnabled,
        boolean mobileVerificationEnabled,
        String emailSubject,
        String messageTemplate) {}
