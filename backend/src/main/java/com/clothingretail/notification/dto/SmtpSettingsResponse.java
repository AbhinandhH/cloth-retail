package com.clothingretail.notification.dto;

/** The real password is never returned - passwordConfigured just says whether one is set. */
public record SmtpSettingsResponse(
        String host,
        int port,
        String username,
        boolean passwordConfigured,
        String fromAddress,
        boolean useStarttls) {}
