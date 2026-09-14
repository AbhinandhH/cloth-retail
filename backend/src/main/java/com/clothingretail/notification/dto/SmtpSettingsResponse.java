package com.clothingretail.notification.dto;

/** The real password/API key is never returned - passwordConfigured/apiKeyConfigured just say whether one is set. */
public record SmtpSettingsResponse(
        String host,
        int port,
        String username,
        boolean passwordConfigured,
        String fromAddress,
        boolean useStarttls,
        String provider,
        boolean apiKeyConfigured) {}
