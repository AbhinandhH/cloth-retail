package com.clothingretail.masterdata.controller;

import com.clothingretail.masterdata.dto.SizeGroupAdminRequest;
import com.clothingretail.masterdata.dto.SizeGroupAdminResponse;
import com.clothingretail.masterdata.service.SizeGroupService;
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
@RequestMapping("/api/admin/size-groups")
@PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
public class AdminSizeGroupController {

    private final SizeGroupService sizeGroupService;

    public AdminSizeGroupController(SizeGroupService sizeGroupService) {
        this.sizeGroupService = sizeGroupService;
    }

    @GetMapping
    public List<SizeGroupAdminResponse> list(
            @RequestParam(required = false) String q, @RequestParam(required = false) Boolean active) {
        return sizeGroupService.listAdmin(q, active);
    }

    @GetMapping("/{id}")
    public SizeGroupAdminResponse get(@PathVariable Long id) {
        return sizeGroupService.getAdmin(id);
    }

    @PostMapping
    public ResponseEntity<SizeGroupAdminResponse> create(@Valid @RequestBody SizeGroupAdminRequest request) {
        return ResponseEntity.ok(sizeGroupService.create(request));
    }

    @PutMapping("/{id}")
    public SizeGroupAdminResponse update(@PathVariable Long id, @Valid @RequestBody SizeGroupAdminRequest request) {
        return sizeGroupService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        sizeGroupService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
