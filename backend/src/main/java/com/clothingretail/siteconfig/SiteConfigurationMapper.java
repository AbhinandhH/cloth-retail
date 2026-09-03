package com.clothingretail.siteconfig;

import com.clothingretail.siteconfig.dto.PublicConfigurationResponse;
import com.clothingretail.siteconfig.dto.SiteConfigurationAdminResponse;
import com.clothingretail.siteconfig.dto.ThemeAdminResponse;
import com.clothingretail.siteconfig.dto.ThemeResponse;

/** Small shared entity-to-DTO mapping, reused by the public and admin configuration/theme controllers. */
final class SiteConfigurationMapper {

    private SiteConfigurationMapper() {}

    static ThemeResponse toThemeResponse(Theme theme) {
        return new ThemeResponse(
                theme.getName(),
                theme.getPrimaryColor(),
                theme.getSecondaryColor(),
                theme.getAccentColor(),
                theme.getBackgroundColor(),
                theme.getTextColor());
    }

    static ThemeAdminResponse toThemeAdminResponse(Theme theme) {
        return new ThemeAdminResponse(
                theme.getId(),
                theme.getName(),
                theme.getPrimaryColor(),
                theme.getSecondaryColor(),
                theme.getAccentColor(),
                theme.getBackgroundColor(),
                theme.getTextColor(),
                theme.getDisplayOrder());
    }

    static PublicConfigurationResponse toPublicResponse(SiteConfiguration config) {
        return new PublicConfigurationResponse(
                toThemeResponse(config.getActiveTheme()),
                config.getBusinessName(),
                config.getTagline(),
                config.getLogoUrl(),
                config.getFaviconUrl(),
                config.getContactEmail(),
                config.getContactPhone(),
                config.getInstagramUrl(),
                config.getWhatsappNumber(),
                config.getFacebookUrl(),
                config.getFooterText(),
                config.getLoginBackgroundImageUrl(),
                config.getLoginPromoImageUrl(),
                config.getLoginPromoText(),
                config.getRegistrationImageUrl());
    }

    static SiteConfigurationAdminResponse toAdminResponse(SiteConfiguration config) {
        return new SiteConfigurationAdminResponse(
                config.getId(),
                toThemeAdminResponse(config.getActiveTheme()),
                config.getBusinessName(),
                config.getTagline(),
                config.getLogoUrl(),
                config.getFaviconUrl(),
                config.getContactEmail(),
                config.getContactPhone(),
                config.getInstagramUrl(),
                config.getWhatsappNumber(),
                config.getFacebookUrl(),
                config.getFooterText(),
                config.getLoginBackgroundImageUrl(),
                config.getLoginPromoImageUrl(),
                config.getLoginPromoText(),
                config.getRegistrationImageUrl());
    }
}
