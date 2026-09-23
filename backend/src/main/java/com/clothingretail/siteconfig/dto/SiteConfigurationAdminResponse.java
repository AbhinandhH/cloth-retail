package com.clothingretail.siteconfig.dto;

/** Full singleton configuration row, including which theme is active - used to pre-fill the admin edit form. */
public record SiteConfigurationAdminResponse(
        Long id,
        ThemeAdminResponse activeTheme,
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
        String registrationImageUrl,
        Integer orderReservationTtlMinutes,
        Boolean reserveStockOnlyAtPayment,
        Integer idleTimeoutMinutes,
        String gstin,
        String registeredAddress) {}
