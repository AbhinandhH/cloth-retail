package com.clothingretail.masterdata.controller;

import com.clothingretail.masterdata.dto.SizeAdminRequest;
import com.clothingretail.masterdata.dto.SizeAdminResponse;
import com.clothingretail.masterdata.service.SizeService;
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
@RequestMapping("/api/admin/sizes")
@PreAuthorize("hasRole('ADMIN') or (hasRole('EMPLOYEE') and @modulePermission.hasAccess('MASTERS'))")
public class AdminSizeController {

    private final SizeService sizeService;

    public AdminSizeController(SizeService sizeService) {
        this.sizeService = sizeService;
    }

    @GetMapping
    public List<SizeAdminResponse> list(
            @RequestParam(required = false) String q, @RequestParam(required = false) Boolean active) {
        return sizeService.listAdmin(q, active);
    }

    @GetMapping("/{id}")
    public SizeAdminResponse get(@PathVariable Long id) {
        return sizeService.getAdmin(id);
    }

    @PostMapping
    public ResponseEntity<SizeAdminResponse> create(@Valid @RequestBody SizeAdminRequest request) {
        return ResponseEntity.ok(sizeService.create(request));
    }

    @PutMapping("/{id}")
    public SizeAdminResponse update(@PathVariable Long id, @Valid @RequestBody SizeAdminRequest request) {
        return sizeService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        sizeService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
