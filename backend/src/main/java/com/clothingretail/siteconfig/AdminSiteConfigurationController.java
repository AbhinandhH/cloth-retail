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

    private final SiteConfigurationService siteConfigurationService;

    public AdminSiteConfigurationController(SiteConfigurationService siteConfigurationService) {
        this.siteConfigurationService = siteConfigurationService;
    }

    @GetMapping
    public SiteConfigurationAdminResponse get() {
        return siteConfigurationService.getAdmin();
    }

    @PutMapping
    public SiteConfigurationAdminResponse update(@Valid @RequestBody SiteConfigurationUpdateRequest request) {
        return siteConfigurationService.update(request);
    }
}
