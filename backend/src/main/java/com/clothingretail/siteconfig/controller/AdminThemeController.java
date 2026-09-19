package com.clothingretail.siteconfig.controller;

import com.clothingretail.siteconfig.dto.PublicConfigurationResponse;
import com.clothingretail.siteconfig.dto.ThemeAdminRequest;
import com.clothingretail.siteconfig.dto.ThemeAdminResponse;
import com.clothingretail.siteconfig.service.ThemeService;
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
@PreAuthorize("hasRole('ADMIN')")
public class AdminThemeController {

    private final ThemeService themeService;

    public AdminThemeController(ThemeService themeService) {
        this.themeService = themeService;
    }

    @GetMapping
    public List<ThemeAdminResponse> list() {
        return themeService.listAdmin();
    }

    @PostMapping
    public ResponseEntity<ThemeAdminResponse> create(@Valid @RequestBody ThemeAdminRequest request) {
        return ResponseEntity.ok(themeService.create(request));
    }

    @PutMapping("/{id}")
    public ThemeAdminResponse update(@PathVariable Long id, @Valid @RequestBody ThemeAdminRequest request) {
        return themeService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        themeService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/activate")
    public PublicConfigurationResponse activate(@PathVariable Long id) {
        return themeService.activate(id);
    }
}
