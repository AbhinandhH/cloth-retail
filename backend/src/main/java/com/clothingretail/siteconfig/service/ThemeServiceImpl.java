package com.clothingretail.siteconfig.service;

import com.clothingretail.common.ConflictException;
import com.clothingretail.common.NotFoundException;
import com.clothingretail.siteconfig.SiteConfiguration;
import com.clothingretail.siteconfig.Theme;
import com.clothingretail.siteconfig.dto.PublicConfigurationResponse;
import com.clothingretail.siteconfig.dto.ThemeAdminRequest;
import com.clothingretail.siteconfig.dto.ThemeAdminResponse;
import com.clothingretail.siteconfig.dto.ThemeResponse;
import com.clothingretail.siteconfig.repository.SiteConfigurationRepository;
import com.clothingretail.siteconfig.repository.ThemeRepository;
import java.util.List;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@Log4j2
public class ThemeServiceImpl implements ThemeService {

    private final ThemeRepository themeRepository;
    private final SiteConfigurationRepository siteConfigurationRepository;

    public ThemeServiceImpl(ThemeRepository themeRepository, SiteConfigurationRepository siteConfigurationRepository) {
        this.themeRepository = themeRepository;
        this.siteConfigurationRepository = siteConfigurationRepository;
    }

    @Override
    public List<ThemeAdminResponse> listAdmin() {
        List<ThemeAdminResponse> themes = themeRepository.findAllByOrderByDisplayOrderAscNameAsc().stream()
                .map(this::toAdminResponse)
                .toList();
        log.info("[1667] Listed {} themes", themes.size());
        return themes;
    }

    @Override
    @Transactional
    public ThemeAdminResponse create(ThemeAdminRequest request) {
        log.info("[1668] Creating theme: name={}", request.name());
        if (themeRepository.existsByNameIgnoreCase(request.name())) {
            log.error("[1669] Theme creation rejected: name {} already exists", request.name());
            throw new ConflictException("A theme with this name already exists");
        }
        Theme theme = new Theme();
        apply(theme, request);
        ThemeAdminResponse response = toAdminResponse(themeRepository.save(theme));
        log.info("[1670] Theme created: id={}, name={}", response.id(), request.name());
        return response;
    }

    @Override
    @Transactional
    public ThemeAdminResponse update(Long id, ThemeAdminRequest request) {
        log.info("[1671] Updating theme: id={}, name={}", id, request.name());
        Theme theme = find(id);
        apply(theme, request);
        ThemeAdminResponse response = toAdminResponse(themeRepository.save(theme));
        log.info("[1672] Theme updated: id={}, name={}", id, request.name());
        return response;
    }

    @Override
    @Transactional
    public void delete(Long id) {
        log.info("[1673] Deleting theme: id={}", id);
        Theme theme = find(id);
        SiteConfiguration config = loadSingleton();
        if (config.getActiveTheme().getId().equals(theme.getId())) {
            log.error("[1674] Theme deletion rejected: theme {} is the currently active theme", id);
            throw new ConflictException("Cannot delete the currently active theme");
        }
        themeRepository.delete(theme);
        log.info("[1675] Theme deleted: id={}", id);
    }

    @Override
    @Transactional
    public PublicConfigurationResponse activate(Long id) {
        log.info("[1676] Activating theme: id={}", id);
        Theme theme = find(id);
        SiteConfiguration config = loadSingleton();
        config.setActiveTheme(theme);
        PublicConfigurationResponse response = toPublicResponse(siteConfigurationRepository.save(config));
        log.info("[1677] Theme activated: id={}, name={}", id, theme.getName());
        return response;
    }

    private Theme find(Long id) {
        return themeRepository.findById(id).orElseThrow(() -> {
            log.error("[1678] Theme not found: id={}", id);
            return new NotFoundException("Theme not found: " + id);
        });
    }

    private SiteConfiguration loadSingleton() {
        return siteConfigurationRepository.findById(SiteConfiguration.SINGLETON_ID)
                .orElseThrow(() -> {
                    log.error("[1679] Singleton site_configuration row (id={}) is missing", SiteConfiguration.SINGLETON_ID);
                    return new IllegalStateException(
                            "Singleton site_configuration row (id=1) is missing - this is a startup-time misconfiguration, "
                                    + "check that V4__init_site_configuration.sql ran");
                });
    }

    private void apply(Theme theme, ThemeAdminRequest request) {
        theme.setName(request.name());
        theme.setPrimaryColor(request.primaryColor());
        theme.setSecondaryColor(request.secondaryColor());
        theme.setAccentColor(request.accentColor());
        theme.setBackgroundColor(request.backgroundColor());
        theme.setTextColor(request.textColor());
        theme.setDisplayOrder(request.displayOrder() != null ? request.displayOrder() : 0);
        theme.setRichAmbient(request.richAmbient() != null ? request.richAmbient() : true);
        theme.setMotif(request.motif() != null && !request.motif().isBlank() ? request.motif() : "SIGNATURE");
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
                theme.getDisplayOrder(),
                theme.isRichAmbient(),
                theme.getMotif());
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
                theme.getTextColor(),
                theme.isRichAmbient(),
                theme.getMotif());
    }
}
