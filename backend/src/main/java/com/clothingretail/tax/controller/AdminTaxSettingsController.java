package com.clothingretail.tax.controller;

import com.clothingretail.tax.dto.TaxSettingsResponse;
import com.clothingretail.tax.dto.TaxSettingsUpdateRequest;
import com.clothingretail.tax.service.TaxSettingsService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Admin-configurable GST rates - see com.clothingretail.tax.TaxSettings. SUPER_ADMIN only, same as the rest of the store-setup modules (SMTP, Notifications, Site configuration). */
@RestController
@RequestMapping("/api/admin/tax-settings")
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class AdminTaxSettingsController {

    private final TaxSettingsService taxSettingsService;

    public AdminTaxSettingsController(TaxSettingsService taxSettingsService) {
        this.taxSettingsService = taxSettingsService;
    }

    @GetMapping
    public TaxSettingsResponse get() {
        return taxSettingsService.get();
    }

    @PutMapping
    public TaxSettingsResponse update(@Valid @RequestBody TaxSettingsUpdateRequest request) {
        return taxSettingsService.update(request);
    }
}
