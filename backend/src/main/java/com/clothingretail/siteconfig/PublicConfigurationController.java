package com.clothingretail.siteconfig;

import com.clothingretail.siteconfig.dto.PublicConfigurationResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** Public, read-only lookup of the current branding/theme - used by the storefront to render itself. No auth required. */
@RestController
public class PublicConfigurationController {

    private final SiteConfigurationRepository siteConfigurationRepository;

    public PublicConfigurationController(SiteConfigurationRepository siteConfigurationRepository) {
        this.siteConfigurationRepository = siteConfigurationRepository;
    }

    @GetMapping("/api/configuration")
    public PublicConfigurationResponse configuration() {
        SiteConfiguration config = siteConfigurationRepository.findById(SiteConfiguration.SINGLETON_ID)
                .orElseThrow(() -> new IllegalStateException(
                        "Singleton site_configuration row (id=1) is missing - this is a startup-time misconfiguration, "
                                + "check that V4__init_site_configuration.sql ran"));
        return SiteConfigurationMapper.toPublicResponse(config);
    }
}
