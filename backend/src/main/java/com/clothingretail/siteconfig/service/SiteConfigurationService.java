package com.clothingretail.siteconfig.service;

import com.clothingretail.siteconfig.dto.PublicConfigurationResponse;
import com.clothingretail.siteconfig.dto.SiteConfigurationAdminResponse;
import com.clothingretail.siteconfig.dto.SiteConfigurationUpdateRequest;

/**
 * Owns the singleton site_configuration row (branding, contact info,
 * login-page visuals) - reads for the public and admin endpoints, and the
 * admin update. The active theme itself only changes via
 * {@link ThemeService#activate}.
 */
public interface SiteConfigurationService {

    SiteConfigurationAdminResponse getAdmin();

    PublicConfigurationResponse getPublic();

    SiteConfigurationAdminResponse update(SiteConfigurationUpdateRequest request);
}
