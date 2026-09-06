package com.clothingretail.siteconfig;

import com.clothingretail.siteconfig.dto.PublicConfigurationResponse;
import com.clothingretail.siteconfig.dto.SiteConfigurationAdminResponse;
import com.clothingretail.siteconfig.dto.SiteConfigurationUpdateRequest;
import com.clothingretail.siteconfig.dto.ThemeAdminResponse;
import com.clothingretail.siteconfig.dto.ThemeResponse;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Owns the singleton site_configuration row (branding, contact info,
 * login-page visuals) - reads for the public and admin endpoints, and the
 * admin update. The active theme itself only changes via
 * {@link ThemeService#activate}.
 */
@Service
@Transactional(readOnly = true)
@Log4j2
public class SiteConfigurationService {

    private final SiteConfigurationRepository repository;

    public SiteConfigurationService(SiteConfigurationRepository repository) {
        this.repository = repository;
    }

    public SiteConfigurationAdminResponse getAdmin() {
        log.info("[1662] Fetching admin site configuration");
        return toAdminResponse(loadSingleton());
    }

    public PublicConfigurationResponse getPublic() {
        log.info("[1663] Fetching public site configuration");
        return toPublicResponse(loadSingleton());
    }

    @Transactional
    public SiteConfigurationAdminResponse update(SiteConfigurationUpdateRequest request) {
        log.info("[1664] Updating site configuration: businessName={}, contactEmail={}, contactPhone={}",
                request.businessName(), request.contactEmail(), request.contactPhone());
        SiteConfiguration config = loadSingleton();
        config.setBusinessName(request.businessName());
        config.setTagline(request.tagline());
        config.setLogoUrl(request.logoUrl());
        config.setFaviconUrl(request.faviconUrl());
        config.setContactEmail(request.contactEmail());
        config.setContactPhone(request.contactPhone());
        config.setInstagramUrl(request.instagramUrl());
        config.setWhatsappNumber(request.whatsappNumber());
        config.setFacebookUrl(request.facebookUrl());
        config.setFooterText(request.footerText());
        config.setLoginBackgroundImageUrl(request.loginBackgroundImageUrl());
        config.setLoginPromoImageUrl(request.loginPromoImageUrl());
        config.setLoginPromoText(request.loginPromoText());
        config.setRegistrationImageUrl(request.registrationImageUrl());
        config = repository.save(config);
        log.info("[1665] Site configuration updated: id={}, businessName={}", config.getId(), config.getBusinessName());
        return toAdminResponse(config);
    }

    private SiteConfiguration loadSingleton() {
        return repository.findById(SiteConfiguration.SINGLETON_ID)
                .orElseThrow(() -> {
                    log.error("[1666] Singleton site_configuration row (id={}) is missing", SiteConfiguration.SINGLETON_ID);
                    return new IllegalStateException(
                            "Singleton site_configuration row (id=1) is missing - this is a startup-time misconfiguration, "
                                    + "check that V4__init_site_configuration.sql ran");
                });
    }

    private PublicConfigurationResponse toPublicResponse(SiteConfiguration config) {
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

    private SiteConfigurationAdminResponse toAdminResponse(SiteConfiguration config) {
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

    private ThemeResponse toThemeResponse(Theme theme) {
        return new ThemeResponse(
                theme.getName(),
                theme.getPrimaryColor(),
                theme.getSecondaryColor(),
                theme.getAccentColor(),
                theme.getBackgroundColor(),
                theme.getTextColor());
    }

    private ThemeAdminResponse toThemeAdminResponse(Theme theme) {
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
}
