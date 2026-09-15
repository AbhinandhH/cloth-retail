package com.clothingretail.siteconfig.service;

import com.clothingretail.siteconfig.dto.PublicConfigurationResponse;
import com.clothingretail.siteconfig.dto.ThemeAdminRequest;
import com.clothingretail.siteconfig.dto.ThemeAdminResponse;
import java.util.List;

/**
 * Theme CRUD, plus the activate-theme workflow: activating a theme also
 * updates the singleton SiteConfiguration row's active-theme pointer,
 * so this service reaches into SiteConfigurationRepository directly
 * for that one operation (mirroring how ProductService reaches into other
 * modules' repositories for FK lookups).
 */
public interface ThemeService {

    List<ThemeAdminResponse> listAdmin();

    ThemeAdminResponse create(ThemeAdminRequest request);

    ThemeAdminResponse update(Long id, ThemeAdminRequest request);

    void delete(Long id);

    PublicConfigurationResponse activate(Long id);
}
