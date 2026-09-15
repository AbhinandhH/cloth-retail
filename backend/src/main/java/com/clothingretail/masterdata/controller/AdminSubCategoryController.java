package com.clothingretail.masterdata.controller;

import com.clothingretail.masterdata.dto.SubCategoryAdminRequest;
import com.clothingretail.masterdata.dto.SubCategoryAdminResponse;
import com.clothingretail.masterdata.service.SubCategoryService;
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
@RequestMapping("/api/admin/sub-categories")
@PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
public class AdminSubCategoryController {

    private final SubCategoryService subCategoryService;

    public AdminSubCategoryController(SubCategoryService subCategoryService) {
        this.subCategoryService = subCategoryService;
    }

    @GetMapping
    public List<SubCategoryAdminResponse> list(
            @RequestParam(required = false) String q, @RequestParam(required = false) Boolean active) {
        return subCategoryService.listAdmin(q, active);
    }

    @GetMapping("/{id}")
    public SubCategoryAdminResponse get(@PathVariable Long id) {
        return subCategoryService.getAdmin(id);
    }

    @PostMapping
    public ResponseEntity<SubCategoryAdminResponse> create(@Valid @RequestBody SubCategoryAdminRequest request) {
        return ResponseEntity.ok(subCategoryService.create(request));
    }

    @PutMapping("/{id}")
    public SubCategoryAdminResponse update(@PathVariable Long id, @Valid @RequestBody SubCategoryAdminRequest request) {
        return subCategoryService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        subCategoryService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
