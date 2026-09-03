package com.clothingretail.siteconfig;

import com.clothingretail.common.ConflictException;
import com.clothingretail.common.NotFoundException;
import com.clothingretail.siteconfig.dto.PublicConfigurationResponse;
import com.clothingretail.siteconfig.dto.ThemeAdminRequest;
import com.clothingretail.siteconfig.dto.ThemeAdminResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Theme CRUD + activation. SUPER_ADMIN only - unlike product/inventory/order
 * management, theme/branding control is not delegable to plain ADMIN accounts.
 */
@RestController
@RequestMapping("/api/admin/themes")
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class AdminThemeController {

    private final ThemeRepository themeRepository;
    private final SiteConfigurationRepository siteConfigurationRepository;

    public AdminThemeController(ThemeRepository themeRepository, SiteConfigurationRepository siteConfigurationRepository) {
        this.themeRepository = themeRepository;
        this.siteConfigurationRepository = siteConfigurationRepository;
    }

    @GetMapping
    public List<ThemeAdminResponse> list() {
        return themeRepository.findAllByOrderByDisplayOrderAscNameAsc().stream()
                .map(SiteConfigurationMapper::toThemeAdminResponse)
                .toList();
    }

    @PostMapping
    public ResponseEntity<ThemeAdminResponse> create(@Valid @RequestBody ThemeAdminRequest request) {
        if (themeRepository.existsByNameIgnoreCase(request.name())) {
            throw new ConflictException("A theme with this name already exists");
        }
        Theme theme = new Theme();
        apply(theme, request);
        return ResponseEntity.ok(SiteConfigurationMapper.toThemeAdminResponse(themeRepository.save(theme)));
    }

    @PutMapping("/{id}")
    public ThemeAdminResponse update(@PathVariable Long id, @Valid @RequestBody ThemeAdminRequest request) {
        Theme theme = find(id);
        apply(theme, request);
        return SiteConfigurationMapper.toThemeAdminResponse(themeRepository.save(theme));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        Theme theme = find(id);
        SiteConfiguration config = loadSingleton();
        if (config.getActiveTheme().getId().equals(theme.getId())) {
            throw new ConflictException("Cannot delete the currently active theme");
        }
        themeRepository.delete(theme);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/activate")
    public PublicConfigurationResponse activate(@PathVariable Long id) {
        Theme theme = find(id);
        SiteConfiguration config = loadSingleton();
        config.setActiveTheme(theme);
        return SiteConfigurationMapper.toPublicResponse(siteConfigurationRepository.save(config));
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
}
