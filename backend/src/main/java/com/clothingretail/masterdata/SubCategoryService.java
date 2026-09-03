package com.clothingretail.masterdata;

import com.clothingretail.common.ConflictException;
import com.clothingretail.common.NotFoundException;
import com.clothingretail.masterdata.dto.SubCategoryAdminRequest;
import com.clothingretail.masterdata.dto.SubCategoryAdminResponse;
import com.clothingretail.masterdata.dto.SubCategoryResponse;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class SubCategoryService {

    private final SubCategoryRepository repository;
    private final CategoryRepository categoryRepository;

    public SubCategoryService(SubCategoryRepository repository, CategoryRepository categoryRepository) {
        this.repository = repository;
        this.categoryRepository = categoryRepository;
    }

    public List<SubCategoryAdminResponse> listAdmin() {
        return repository.findAll().stream().map(this::toResponse).toList();
    }

    public SubCategoryAdminResponse getAdmin(Long id) {
        return toResponse(find(id));
    }

    public List<SubCategoryResponse> listPublic(Long categoryId) {
        List<SubCategory> subCategories = categoryId != null
                ? repository.findByCategoryIdAndActiveTrueOrderByDisplayOrderAscNameAsc(categoryId)
                : repository.findByActiveTrueOrderByDisplayOrderAscNameAsc();
        return subCategories.stream()
                .map(sc -> new SubCategoryResponse(sc.getId(), sc.getName(), sc.getSlug(), sc.getCategory().getId()))
                .toList();
    }

    @Transactional
    public SubCategoryAdminResponse create(SubCategoryAdminRequest request) {
        if (repository.existsBySlugIgnoreCase(request.slug())) {
            throw new ConflictException("A sub-category with this slug already exists");
        }
        SubCategory subCategory = new SubCategory();
        apply(subCategory, request);
        return toResponse(repository.save(subCategory));
    }

    @Transactional
    public SubCategoryAdminResponse update(Long id, SubCategoryAdminRequest request) {
        SubCategory subCategory = find(id);
        apply(subCategory, request);
        return toResponse(repository.save(subCategory));
    }

    @Transactional
    public void delete(Long id) {
        repository.delete(find(id));
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
