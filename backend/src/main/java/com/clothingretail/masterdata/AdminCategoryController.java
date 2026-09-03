package com.clothingretail.masterdata;

import com.clothingretail.masterdata.dto.CategoryAdminRequest;
import com.clothingretail.masterdata.dto.CategoryAdminResponse;
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

@RestController
@RequestMapping("/api/admin/categories")
@PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
public class AdminCategoryController {

    private final CategoryService categoryService;

    public AdminCategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    public List<CategoryAdminResponse> list() {
        return categoryService.listAdmin();
    }

    @GetMapping("/{id}")
    public CategoryAdminResponse get(@PathVariable Long id) {
        return categoryService.getAdmin(id);
    }

    @PostMapping
    public ResponseEntity<CategoryAdminResponse> create(@Valid @RequestBody CategoryAdminRequest request) {
        return ResponseEntity.ok(categoryService.create(request));
    }

    @PutMapping("/{id}")
    public CategoryAdminResponse update(@PathVariable Long id, @Valid @RequestBody CategoryAdminRequest request) {
        return categoryService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        categoryService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
