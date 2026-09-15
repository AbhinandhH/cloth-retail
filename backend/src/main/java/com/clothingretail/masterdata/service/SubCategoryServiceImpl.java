package com.clothingretail.masterdata.service;

import com.clothingretail.common.AuditorNameResolver;
import com.clothingretail.common.ConflictException;
import com.clothingretail.common.NotFoundException;
import com.clothingretail.masterdata.Category;
import com.clothingretail.masterdata.SubCategory;
import com.clothingretail.masterdata.dto.SubCategoryAdminRequest;
import com.clothingretail.masterdata.dto.SubCategoryAdminResponse;
import com.clothingretail.masterdata.dto.SubCategoryResponse;
import com.clothingretail.masterdata.repository.CategoryRepository;
import com.clothingretail.masterdata.repository.SubCategoryRepository;
import com.clothingretail.product.ProductRepository;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Transactional(readOnly = true)
@Log4j2
public class SubCategoryServiceImpl implements SubCategoryService {

    private final SubCategoryRepository repository;
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final AuditorNameResolver auditorNameResolver;

    public SubCategoryServiceImpl(
            SubCategoryRepository repository,
            CategoryRepository categoryRepository,
            ProductRepository productRepository,
            AuditorNameResolver auditorNameResolver) {
        this.repository = repository;
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
        this.auditorNameResolver = auditorNameResolver;
    }

    @Override
    public List<SubCategoryAdminResponse> listAdmin(String q, Boolean active) {
        log.info("[1457] Listing sub-categories query={} active={}", q, active);
        List<SubCategory> subCategories = repository.findAll().stream()
                .filter(sc -> matchesQuery(sc, q))
                .filter(sc -> active == null || sc.isActive() == active)
                .toList();
        Map<Long, String> names = auditorNameResolver.resolveNames(
                subCategories.stream().flatMap(sc -> Stream.of(sc.getCreatedBy(), sc.getUpdatedBy())).toList());
        return subCategories.stream().map(sc -> toResponse(sc, names)).toList();
    }

    @Override
    public SubCategoryAdminResponse getAdmin(Long id) {
        log.info("[1458] Fetching sub-category id={}", id);
        SubCategory subCategory = find(id);
        return toResponse(subCategory, auditorNameResolver.resolveNames(subCategory.getCreatedBy(), subCategory.getUpdatedBy()));
    }

    @Override
    public List<SubCategoryResponse> listPublic(Long categoryId) {
        log.info("[1459] Listing public sub-categories categoryId={}", categoryId);
        List<SubCategory> subCategories = categoryId != null
                ? repository.findByCategoryIdAndActiveTrueOrderByDisplayOrderAscNameAsc(categoryId)
                : repository.findByActiveTrueOrderByDisplayOrderAscNameAsc();
        return subCategories.stream()
                .map(sc -> new SubCategoryResponse(sc.getId(), sc.getName(), sc.getSlug(), sc.getCategory().getId()))
                .toList();
    }

    @Override
    @Transactional
    public SubCategoryAdminResponse create(SubCategoryAdminRequest request) {
        log.info("[1460] Creating sub-category name={} slug={} categoryId={} displayOrder={} active={}", request.name(), request.slug(), request.categoryId(), request.displayOrder(), request.active());
        if (repository.existsBySlugIgnoreCase(request.slug())) {
            log.error("[1461] Cannot create sub-category - slug={} already exists", request.slug());
            throw new ConflictException("A sub-category with this slug already exists");
        }
        SubCategory subCategory = new SubCategory();
        apply(subCategory, request);
        SubCategory saved = repository.save(subCategory);
        return toResponse(saved, auditorNameResolver.resolveNames(saved.getCreatedBy(), saved.getUpdatedBy()));
    }

    @Override
    @Transactional
    public SubCategoryAdminResponse update(Long id, SubCategoryAdminRequest request) {
        log.info("[1462] Updating sub-category id={} name={} slug={} categoryId={} displayOrder={} active={}", id, request.name(), request.slug(), request.categoryId(), request.displayOrder(), request.active());
        SubCategory subCategory = find(id);
        apply(subCategory, request);
        SubCategory saved = repository.save(subCategory);
        return toResponse(saved, auditorNameResolver.resolveNames(saved.getCreatedBy(), saved.getUpdatedBy()));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        log.info("[1463] Deleting sub-category id={}", id);
        SubCategory subCategory = find(id);
        long usageCount = productRepository.countBySubCategoryId(id);
        if (usageCount > 0) {
            log.error("[1464] Cannot delete sub-category id={} - {} products depend on it", id, usageCount);
            throw new ConflictException(
                    "This sub-category is currently used by " + usageCount + " products and cannot be deleted. Deactivate it instead.");
        }
        repository.delete(subCategory);
    }

    private SubCategory find(Long id) {
        return repository.findById(id).orElseThrow(() -> {
            log.error("[1465] Sub-category not found id={}", id);
            return new NotFoundException("Sub-category not found: " + id);
        });
    }

    private boolean matchesQuery(SubCategory subCategory, String q) {
        return !StringUtils.hasText(q) || subCategory.getName().toLowerCase().contains(q.toLowerCase());
    }

    private void apply(SubCategory subCategory, SubCategoryAdminRequest request) {
        Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> {
                    log.error("[1466] Cannot save sub-category - categoryId={} not found", request.categoryId());
                    return new NotFoundException("Category not found: " + request.categoryId());
                });
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
