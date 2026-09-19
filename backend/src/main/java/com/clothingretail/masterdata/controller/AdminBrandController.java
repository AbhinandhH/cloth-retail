package com.clothingretail.masterdata.controller;

import com.clothingretail.masterdata.dto.BrandAdminRequest;
import com.clothingretail.masterdata.dto.BrandAdminResponse;
import com.clothingretail.masterdata.service.BrandService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/brands")
@PreAuthorize("hasRole('ADMIN') or (hasRole('EMPLOYEE') and @modulePermission.hasAccess('MASTERS'))")
public class AdminBrandController {

    private final BrandService brandService;

    public AdminBrandController(BrandService brandService) {
        this.brandService = brandService;
    }

    @GetMapping
    public List<BrandAdminResponse> list(
            @RequestParam(required = false) String q, @RequestParam(required = false) Boolean active) {
        return brandService.listAdmin(q, active);
    }

    @GetMapping("/{id}")
    public BrandAdminResponse get(@PathVariable Long id) {
        return brandService.getAdmin(id);
    }

    @PostMapping
    public ResponseEntity<BrandAdminResponse> create(@Valid @RequestBody BrandAdminRequest request) {
        return ResponseEntity.ok(brandService.create(request));
    }

    @PutMapping("/{id}")
    public BrandAdminResponse update(@PathVariable Long id, @Valid @RequestBody BrandAdminRequest request) {
        return brandService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        brandService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
