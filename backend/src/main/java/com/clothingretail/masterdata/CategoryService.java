package com.clothingretail.masterdata;

import com.clothingretail.common.ConflictException;
import com.clothingretail.common.NotFoundException;
import com.clothingretail.masterdata.dto.CategoryAdminRequest;
import com.clothingretail.masterdata.dto.CategoryAdminResponse;
import com.clothingretail.masterdata.dto.CategoryResponse;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CategoryService {

    private final CategoryRepository repository;

    public CategoryService(CategoryRepository repository) {
        this.repository = repository;
    }

    public List<CategoryAdminResponse> listAdmin() {
        return repository.findAll().stream().map(this::toResponse).toList();
    }

    public CategoryAdminResponse getAdmin(Long id) {
        return toResponse(find(id));
    }

    public List<CategoryResponse> listPublic() {
        return repository.findByActiveTrueOrderByDisplayOrderAscNameAsc().stream()
                .map(c -> new CategoryResponse(c.getId(), c.getName(), c.getSlug()))
                .toList();
    }

    @Transactional
    public CategoryAdminResponse create(CategoryAdminRequest request) {
        if (repository.existsByNameIgnoreCase(request.name()) || repository.existsBySlugIgnoreCase(request.slug())) {
            throw new ConflictException("A category with this name or slug already exists");
        }
        Category category = new Category();
        apply(category, request);
        return toResponse(repository.save(category));
    }

    @Transactional
    public CategoryAdminResponse update(Long id, CategoryAdminRequest request) {
        Category category = find(id);
        apply(category, request);
        return toResponse(repository.save(category));
    }

    @Transactional
    public void delete(Long id) {
        Category category = find(id);
        repository.delete(category);
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
