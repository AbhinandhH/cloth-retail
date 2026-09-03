package com.clothingretail.siteconfig.dto;

/**
 * Shape returned by the public {@code GET /api/configuration} endpoint and by
 * the admin theme-activate endpoint. Field names are part of the contract the
 * frontend is coded against - do not rename without coordinating.
 */
public record PublicConfigurationResponse(
        ThemeResponse theme,
        String businessName,
        String tagline,
        String logoUrl,
        String faviconUrl,
        String contactEmail,
        String contactPhone,
        String instagramUrl,
        String whatsappNumber,
        String facebookUrl,
        String footerText,
        String loginBackgroundImageUrl,
        String loginPromoImageUrl,
        String loginPromoText,
        String registrationImageUrl) {}
