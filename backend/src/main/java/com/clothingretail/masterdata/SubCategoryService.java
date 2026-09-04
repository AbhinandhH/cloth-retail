package com.clothingretail.masterdata;

import com.clothingretail.common.AuditorNameResolver;
import com.clothingretail.common.ConflictException;
import com.clothingretail.common.NotFoundException;
import com.clothingretail.masterdata.dto.SubCategoryAdminRequest;
import com.clothingretail.masterdata.dto.SubCategoryAdminResponse;
import com.clothingretail.masterdata.dto.SubCategoryResponse;
import com.clothingretail.product.ProductRepository;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Transactional(readOnly = true)
public class SubCategoryService {

    private final SubCategoryRepository repository;
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final AuditorNameResolver auditorNameResolver;

    public SubCategoryService(
            SubCategoryRepository repository,
            CategoryRepository categoryRepository,
            ProductRepository productRepository,
            AuditorNameResolver auditorNameResolver) {
        this.repository = repository;
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
        this.auditorNameResolver = auditorNameResolver;
    }

    public List<SubCategoryAdminResponse> listAdmin(String q, Boolean active) {
        List<SubCategory> subCategories = repository.findAll().stream()
                .filter(sc -> matchesQuery(sc, q))
                .filter(sc -> active == null || sc.isActive() == active)
                .toList();
        Map<Long, String> names = auditorNameResolver.resolveNames(
                subCategories.stream().flatMap(sc -> Stream.of(sc.getCreatedBy(), sc.getUpdatedBy())).toList());
        return subCategories.stream().map(sc -> toResponse(sc, names)).toList();
    }

    public SubCategoryAdminResponse getAdmin(Long id) {
        SubCategory subCategory = find(id);
        return toResponse(subCategory, auditorNameResolver.resolveNames(subCategory.getCreatedBy(), subCategory.getUpdatedBy()));
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
        SubCategory saved = repository.save(subCategory);
        return toResponse(saved, auditorNameResolver.resolveNames(saved.getCreatedBy(), saved.getUpdatedBy()));
    }

    @Transactional
    public SubCategoryAdminResponse update(Long id, SubCategoryAdminRequest request) {
        SubCategory subCategory = find(id);
        apply(subCategory, request);
        SubCategory saved = repository.save(subCategory);
        return toResponse(saved, auditorNameResolver.resolveNames(saved.getCreatedBy(), saved.getUpdatedBy()));
    }

    @Transactional
    public void delete(Long id) {
        SubCategory subCategory = find(id);
        long usageCount = productRepository.countBySubCategoryId(id);
        if (usageCount > 0) {
            throw new ConflictException(
                    "This sub-category is currently used by " + usageCount + " products and cannot be deleted. Deactivate it instead.");
        }
        repository.delete(subCategory);
    }

    private SubCategory find(Long id) {
        return repository.findById(id).orElseThrow(() -> new NotFoundException("Sub-category not found: " + id));
    }

    private boolean matchesQuery(SubCategory subCategory, String q) {
        return !StringUtils.hasText(q) || subCategory.getName().toLowerCase().contains(q.toLowerCase());
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

    private SubCategoryAdminResponse toResponse(SubCategory sc, Map<Long, String> names) {
        return new SubCategoryAdminResponse(
                sc.getId(),
                sc.getName(),
                sc.getSlug(),
                sc.getCategory().getId(),
                sc.getCategory().getName(),
                sc.getDisplayOrder(),
                sc.isActive(),
                names.get(sc.getCreatedBy()),
                names.get(sc.getUpdatedBy()),
                sc.getCreatedAt(),
                sc.getUpdatedAt());
    }
}
