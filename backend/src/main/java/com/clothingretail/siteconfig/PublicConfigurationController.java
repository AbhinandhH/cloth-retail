package com.clothingretail.siteconfig;

import com.clothingretail.siteconfig.dto.PublicConfigurationResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** Public, read-only lookup of the current branding/theme - used by the storefront to render itself. No auth required. */
@RestController
public class PublicConfigurationController {

    private final SiteConfigurationService siteConfigurationService;

    public PublicConfigurationController(SiteConfigurationService siteConfigurationService) {
        this.siteConfigurationService = siteConfigurationService;
    }

    @GetMapping("/api/configuration")
    public PublicConfigurationResponse configuration() {
        return siteConfigurationService.getPublic();
    }
}
