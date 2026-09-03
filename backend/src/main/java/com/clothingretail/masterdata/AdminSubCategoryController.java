package com.clothingretail.masterdata;

import com.clothingretail.common.ConflictException;
import com.clothingretail.common.NotFoundException;
import com.clothingretail.masterdata.dto.SubCategoryAdminRequest;
import com.clothingretail.masterdata.dto.SubCategoryAdminResponse;
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
@RequestMapping("/api/admin/sub-categories")
@PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
public class AdminSubCategoryController {

    private final SubCategoryRepository repository;
    private final CategoryRepository categoryRepository;

    public AdminSubCategoryController(SubCategoryRepository repository, CategoryRepository categoryRepository) {
        this.repository = repository;
        this.categoryRepository = categoryRepository;
    }

    @GetMapping
    public List<SubCategoryAdminResponse> list() {
        return repository.findAll().stream().map(this::toResponse).toList();
    }

    @GetMapping("/{id}")
    public SubCategoryAdminResponse get(@PathVariable Long id) {
        return toResponse(find(id));
    }

    @PostMapping
    public ResponseEntity<SubCategoryAdminResponse> create(@Valid @RequestBody SubCategoryAdminRequest request) {
        if (repository.existsBySlugIgnoreCase(request.slug())) {
            throw new ConflictException("A sub-category with this slug already exists");
        }
        SubCategory subCategory = new SubCategory();
        apply(subCategory, request);
        return ResponseEntity.ok(toResponse(repository.save(subCategory)));
    }

    @PutMapping("/{id}")
    public SubCategoryAdminResponse update(@PathVariable Long id, @Valid @RequestBody SubCategoryAdminRequest request) {
        SubCategory subCategory = find(id);
        apply(subCategory, request);
        return toResponse(repository.save(subCategory));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        repository.delete(find(id));
        return ResponseEntity.noContent().build();
    }

    private SubCategory find(Long id) {
        return repository.findById(id).orElseThrow(() -> new NotFoundException("Sub-category not found: " + id));
    }

    private void apply(SubCategory subCategory, SubCategoryAdminRequest request) {
        Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new NotFoundException("Category not found: " + request.categoryId()));
        subCategory.setCategory(category);
        subCategory.setName(request.name());
        subCategory.setSlug(request.slug());
        subCategory.setDisplayOrder(request.displayOrder() != null ? request.displayOrder() : 0);
        subCategory.setActive(request.active());
    }

    private SubCategoryAdminResponse toResponse(SubCategory sc) {
        return new SubCategoryAdminResponse(
                sc.getId(), sc.getName(), sc.getSlug(), sc.getCategory().getId(), sc.getCategory().getName(), sc.getDisplayOrder(), sc.isActive());
    }
}
