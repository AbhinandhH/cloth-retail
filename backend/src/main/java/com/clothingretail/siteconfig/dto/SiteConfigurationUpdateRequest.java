package com.clothingretail.siteconfig.dto;

import jakarta.validation.constraints.NotBlank;

/** Body for {@code PUT /api/admin/configuration}. activeTheme is deliberately excluded - it only changes via POST /api/admin/themes/{id}/activate. */
public record SiteConfigurationUpdateRequest(
        @NotBlank(message = "must not be blank") String businessName,
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
