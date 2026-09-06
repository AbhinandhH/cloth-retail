package com.clothingretail.masterdata;

import com.clothingretail.common.AuditorNameResolver;
import com.clothingretail.common.ConflictException;
import com.clothingretail.common.NotFoundException;
import com.clothingretail.masterdata.dto.CategoryAdminRequest;
import com.clothingretail.masterdata.dto.CategoryAdminResponse;
import com.clothingretail.masterdata.dto.CategoryResponse;
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
public class CategoryService {

    private final CategoryRepository repository;
    private final SubCategoryRepository subCategoryRepository;
    private final ProductRepository productRepository;
    private final AuditorNameResolver auditorNameResolver;

    public CategoryService(
            CategoryRepository repository,
            SubCategoryRepository subCategoryRepository,
            ProductRepository productRepository,
            AuditorNameResolver auditorNameResolver) {
        this.repository = repository;
        this.subCategoryRepository = subCategoryRepository;
        this.productRepository = productRepository;
        this.auditorNameResolver = auditorNameResolver;
    }

    public List<CategoryAdminResponse> listAdmin(String q, Boolean active) {
        log.info("[1408] Listing categories query={} active={}", q, active);
        List<Category> categories = repository.findAll().stream()
                .filter(c -> matchesQuery(c, q))
                .filter(c -> active == null || c.isActive() == active)
                .toList();
        Map<Long, String> names = auditorNameResolver.resolveNames(
                categories.stream().flatMap(c -> Stream.of(c.getCreatedBy(), c.getUpdatedBy())).toList());
        return categories.stream().map(c -> toResponse(c, names)).toList();
    }

    public CategoryAdminResponse getAdmin(Long id) {
        log.info("[1409] Fetching category id={}", id);
        Category category = find(id);
        return toResponse(category, auditorNameResolver.resolveNames(category.getCreatedBy(), category.getUpdatedBy()));
    }

    public List<CategoryResponse> listPublic() {
        log.info("[1410] Listing public categories");
        return repository.findByActiveTrueOrderByDisplayOrderAscNameAsc().stream()
                .map(c -> new CategoryResponse(c.getId(), c.getName(), c.getSlug()))
                .toList();
    }

    @Transactional
    public CategoryAdminResponse create(CategoryAdminRequest request) {
        log.info("[1411] Creating category name={} slug={} displayOrder={} active={}", request.name(), request.slug(), request.displayOrder(), request.active());
        if (repository.existsByNameIgnoreCase(request.name()) || repository.existsBySlugIgnoreCase(request.slug())) {
            log.error("[1412] Cannot create category - name={} or slug={} already exists", request.name(), request.slug());
            throw new ConflictException("A category with this name or slug already exists");
        }
        Category category = new Category();
        apply(category, request);
        Category saved = repository.save(category);
        return toResponse(saved, auditorNameResolver.resolveNames(saved.getCreatedBy(), saved.getUpdatedBy()));
    }

    @Transactional
    public CategoryAdminResponse update(Long id, CategoryAdminRequest request) {
        log.info("[1413] Updating category id={} name={} slug={} displayOrder={} active={}", id, request.name(), request.slug(), request.displayOrder(), request.active());
        Category category = find(id);
        apply(category, request);
        Category saved = repository.save(category);
        return toResponse(saved, auditorNameResolver.resolveNames(saved.getCreatedBy(), saved.getUpdatedBy()));
    }

    @Transactional
    public void delete(Long id) {
        log.info("[1414] Deleting category id={}", id);
        Category category = find(id);
        long productCount = productRepository.countByCategoryId(id);
        if (productCount > 0) {
            log.error("[1415] Cannot delete category id={} - {} products depend on it", id, productCount);
            throw new ConflictException(
                    "This category is currently used by " + productCount + " products and cannot be deleted. Deactivate it instead.");
        }
        long subCategoryCount = subCategoryRepository.countByCategoryId(id);
        if (subCategoryCount > 0) {
            log.error("[1416] Cannot delete category id={} - {} sub-categories depend on it", id, subCategoryCount);
            throw new ConflictException(
                    "This category has " + subCategoryCount + " sub-categories and cannot be deleted. Deactivate it instead.");
        }
        repository.delete(category);
    }

    private Category find(Long id) {
        return repository.findById(id).orElseThrow(() -> {
            log.error("[1417] Category not found id={}", id);
            return new NotFoundException("Category not found: " + id);
        });
    }

    private boolean matchesQuery(Category category, String q) {
        return !StringUtils.hasText(q) || category.getName().toLowerCase().contains(q.toLowerCase());
    }

    private void apply(Category category, CategoryAdminRequest request) {
        category.setName(request.name());
        category.setSlug(request.slug());
        category.setDisplayOrder(request.displayOrder() != null ? request.displayOrder() : 0);
        category.setActive(request.active());
    }

    private CategoryAdminResponse toResponse(Category c, Map<Long, String> names) {
        return new CategoryAdminResponse(
                c.getId(),
                c.getName(),
                c.getSlug(),
                c.getDisplayOrder(),
                c.isActive(),
                names.get(c.getCreatedBy()),
                names.get(c.getUpdatedBy()),
                c.getCreatedAt(),
                c.getUpdatedAt());
    }
}
