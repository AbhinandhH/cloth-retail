package com.clothingretail.masterdata;

import com.clothingretail.common.ConflictException;
import com.clothingretail.common.NotFoundException;
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

    private final CategoryRepository repository;

    public AdminCategoryController(CategoryRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<CategoryAdminResponse> list() {
        return repository.findAll().stream().map(this::toResponse).toList();
    }

    @GetMapping("/{id}")
    public CategoryAdminResponse get(@PathVariable Long id) {
        return toResponse(find(id));
    }

    @PostMapping
    public ResponseEntity<CategoryAdminResponse> create(@Valid @RequestBody CategoryAdminRequest request) {
        if (repository.existsByNameIgnoreCase(request.name()) || repository.existsBySlugIgnoreCase(request.slug())) {
            throw new ConflictException("A category with this name or slug already exists");
        }
        Category category = new Category();
        apply(category, request);
        return ResponseEntity.ok(toResponse(repository.save(category)));
    }

    @PutMapping("/{id}")
    public CategoryAdminResponse update(@PathVariable Long id, @Valid @RequestBody CategoryAdminRequest request) {
        Category category = find(id);
        apply(category, request);
        return toResponse(repository.save(category));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        Category category = find(id);
        repository.delete(category);
        return ResponseEntity.noContent().build();
    }

    private Category find(Long id) {
        return repository.findById(id).orElseThrow(() -> new NotFoundException("Category not found: " + id));
    }

    private void apply(Category category, CategoryAdminRequest request) {
        category.setName(request.name());
        category.setSlug(request.slug());
        category.setDisplayOrder(request.displayOrder() != null ? request.displayOrder() : 0);
        category.setActive(request.active());
    }

    private CategoryAdminResponse toResponse(Category c) {
        return new CategoryAdminResponse(c.getId(), c.getName(), c.getSlug(), c.getDisplayOrder(), c.isActive());
    }
}
