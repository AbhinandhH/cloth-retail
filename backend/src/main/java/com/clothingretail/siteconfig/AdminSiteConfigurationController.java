package com.clothingretail.siteconfig;

import com.clothingretail.siteconfig.dto.SiteConfigurationAdminResponse;
import com.clothingretail.siteconfig.dto.SiteConfigurationUpdateRequest;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Admin read/update of the singleton site configuration row (branding,
 * contact info, login-page visuals). SUPER_ADMIN only. The active theme is
 * not editable here - see AdminThemeController#activate.
 */
@RestController
@RequestMapping("/api/admin/configuration")
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class AdminSiteConfigurationController {

    private final SiteConfigurationRepository siteConfigurationRepository;

    public AdminSiteConfigurationController(SiteConfigurationRepository siteConfigurationRepository) {
        this.siteConfigurationRepository = siteConfigurationRepository;
    }

    @GetMapping
    public SiteConfigurationAdminResponse get() {
        return SiteConfigurationMapper.toAdminResponse(loadSingleton());
    }

    @PutMapping
    public SiteConfigurationAdminResponse update(@Valid @RequestBody SiteConfigurationUpdateRequest request) {
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
        return SiteConfigurationMapper.toAdminResponse(siteConfigurationRepository.save(config));
    }

    private SiteConfiguration loadSingleton() {
        return siteConfigurationRepository.findById(SiteConfiguration.SINGLETON_ID)
                .orElseThrow(() -> new IllegalStateException(
                        "Singleton site_configuration row (id=1) is missing - this is a startup-time misconfiguration, "
                                + "check that V4__init_site_configuration.sql ran"));
    }
}
