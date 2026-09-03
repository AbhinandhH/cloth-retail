package com.clothingretail.siteconfig;

import com.clothingretail.common.ConflictException;
import com.clothingretail.common.NotFoundException;
import com.clothingretail.siteconfig.dto.PublicConfigurationResponse;
import com.clothingretail.siteconfig.dto.ThemeAdminRequest;
import com.clothingretail.siteconfig.dto.ThemeAdminResponse;
import com.clothingretail.siteconfig.dto.ThemeResponse;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Theme CRUD, plus the activate-theme workflow: activating a theme also
 * updates the singleton {@link SiteConfiguration} row's active-theme pointer,
 * so this service reaches into {@link SiteConfigurationRepository} directly
 * for that one operation (mirroring how ProductService reaches into other
 * modules' repositories for FK lookups).
 */
@Service
@Transactional(readOnly = true)
public class ThemeService {

    private final ThemeRepository themeRepository;
    private final SiteConfigurationRepository siteConfigurationRepository;

    public ThemeService(ThemeRepository themeRepository, SiteConfigurationRepository siteConfigurationRepository) {
        this.themeRepository = themeRepository;
        this.siteConfigurationRepository = siteConfigurationRepository;
    }

    public List<ThemeAdminResponse> listAdmin() {
        return themeRepository.findAllByOrderByDisplayOrderAscNameAsc().stream()
                .map(this::toAdminResponse)
                .toList();
    }

    @Transactional
    public ThemeAdminResponse create(ThemeAdminRequest request) {
        if (themeRepository.existsByNameIgnoreCase(request.name())) {
            throw new ConflictException("A theme with this name already exists");
        }
        Theme theme = new Theme();
        apply(theme, request);
        return toAdminResponse(themeRepository.save(theme));
    }

    @Transactional
    public ThemeAdminResponse update(Long id, ThemeAdminRequest request) {
        Theme theme = find(id);
        apply(theme, request);
        return toAdminResponse(themeRepository.save(theme));
    }

    @Transactional
    public void delete(Long id) {
        Theme theme = find(id);
        SiteConfiguration config = loadSingleton();
        if (config.getActiveTheme().getId().equals(theme.getId())) {
            throw new ConflictException("Cannot delete the currently active theme");
        }
        themeRepository.delete(theme);
    }

    @Transactional
    public PublicConfigurationResponse activate(Long id) {
        Theme theme = find(id);
        SiteConfiguration config = loadSingleton();
        config.setActiveTheme(theme);
        return toPublicResponse(siteConfigurationRepository.save(config));
    }

    private Theme find(Long id) {
        return themeRepository.findById(id).orElseThrow(() -> new NotFoundException("Theme not found: " + id));
    }

    private SiteConfiguration loadSingleton() {
        return siteConfigurationRepository.findById(SiteConfiguration.SINGLETON_ID)
                .orElseThrow(() -> new IllegalStateException(
                        "Singleton site_configuration row (id=1) is missing - this is a startup-time misconfiguration, "
                                + "check that V4__init_site_configuration.sql ran"));
    }

    private void apply(Theme theme, ThemeAdminRequest request) {
        theme.setName(request.name());
        theme.setPrimaryColor(request.primaryColor());
        theme.setSecondaryColor(request.secondaryColor());
        theme.setAccentColor(request.accentColor());
        theme.setBackgroundColor(request.backgroundColor());
        theme.setTextColor(request.textColor());
        theme.setDisplayOrder(request.displayOrder() != null ? request.displayOrder() : 0);
    }

    private ThemeAdminResponse toAdminResponse(Theme theme) {
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

    private ThemeResponse toThemeResponse(Theme theme) {
        return new ThemeResponse(
                theme.getName(),
                theme.getPrimaryColor(),
                theme.getSecondaryColor(),
                theme.getAccentColor(),
                theme.getBackgroundColor(),
                theme.getTextColor());
    }
}
