package com.clothingretail.masterdata.controller;

import com.clothingretail.masterdata.dto.SizeChartAdminRequest;
import com.clothingretail.masterdata.dto.SizeChartAdminResponse;
import com.clothingretail.masterdata.service.SizeChartService;
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
@RequestMapping("/api/admin/size-charts")
@PreAuthorize("hasRole('ADMIN') or (hasRole('EMPLOYEE') and @modulePermission.hasAccess('MASTERS'))")
public class AdminSizeChartController {

    private final SizeChartService sizeChartService;

    public AdminSizeChartController(SizeChartService sizeChartService) {
        this.sizeChartService = sizeChartService;
    }

    @GetMapping
    public List<SizeChartAdminResponse> list(
            @RequestParam(required = false) String q, @RequestParam(required = false) Boolean active) {
        return sizeChartService.listAdmin(q, active);
    }

    @GetMapping("/{id}")
    public SizeChartAdminResponse get(@PathVariable Long id) {
        return sizeChartService.getAdmin(id);
    }

    @PostMapping
    public ResponseEntity<SizeChartAdminResponse> create(@Valid @RequestBody SizeChartAdminRequest request) {
        return ResponseEntity.ok(sizeChartService.create(request));
    }

    @PutMapping("/{id}")
    public SizeChartAdminResponse update(@PathVariable Long id, @Valid @RequestBody SizeChartAdminRequest request) {
        return sizeChartService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        sizeChartService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
