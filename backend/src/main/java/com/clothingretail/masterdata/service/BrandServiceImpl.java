package com.clothingretail.masterdata.service;

import com.clothingretail.common.AuditorNameResolver;
import com.clothingretail.common.ConflictException;
import com.clothingretail.common.NotFoundException;
import com.clothingretail.masterdata.Brand;
import com.clothingretail.masterdata.dto.BrandAdminRequest;
import com.clothingretail.masterdata.dto.BrandAdminResponse;
import com.clothingretail.masterdata.repository.BrandRepository;
import com.clothingretail.product.repository.ProductRepository;
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
public class BrandServiceImpl implements BrandService {

    private final BrandRepository repository;
    private final ProductRepository productRepository;
    private final AuditorNameResolver auditorNameResolver;

    public BrandServiceImpl(BrandRepository repository, ProductRepository productRepository, AuditorNameResolver auditorNameResolver) {
        this.repository = repository;
        this.productRepository = productRepository;
        this.auditorNameResolver = auditorNameResolver;
    }

    @Override
    public List<BrandAdminResponse> listAdmin(String q, Boolean active) {
        log.info("[1400] Listing brands query={} active={}", q, active);
        List<Brand> brands = repository.findAll().stream()
                .filter(b -> matchesQuery(b, q))
                .filter(b -> active == null || b.isActive() == active)
                .toList();
        Map<Long, String> names = auditorNameResolver.resolveNames(
                brands.stream().flatMap(b -> Stream.of(b.getCreatedBy(), b.getUpdatedBy())).toList());
        return brands.stream().map(b -> toResponse(b, names)).toList();
    }

    @Override
    public BrandAdminResponse getAdmin(Long id) {
        log.info("[1401] Fetching brand id={}", id);
        Brand brand = find(id);
        return toResponse(brand, auditorNameResolver.resolveNames(brand.getCreatedBy(), brand.getUpdatedBy()));
    }

    @Override
    @Transactional
    public BrandAdminResponse create(BrandAdminRequest request) {
        log.info("[1402] Creating brand name={} displayOrder={} active={}", request.name(), request.displayOrder(), request.active());
        if (repository.existsByNameIgnoreCase(request.name())) {
            log.error("[1403] Cannot create brand - name={} already exists", request.name());
            throw new ConflictException("A brand with this name already exists");
        }
        Brand brand = new Brand();
        apply(brand, request);
        Brand saved = repository.save(brand);
        return toResponse(saved, auditorNameResolver.resolveNames(saved.getCreatedBy(), saved.getUpdatedBy()));
    }

    @Override
    @Transactional
    public BrandAdminResponse update(Long id, BrandAdminRequest request) {
        log.info("[1404] Updating brand id={} name={} displayOrder={} active={}", id, request.name(), request.displayOrder(), request.active());
        Brand brand = find(id);
        apply(brand, request);
        Brand saved = repository.save(brand);
        return toResponse(saved, auditorNameResolver.resolveNames(saved.getCreatedBy(), saved.getUpdatedBy()));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        log.info("[1405] Deleting brand id={}", id);
        Brand brand = find(id);
        long usageCount = productRepository.countByBrandId(id);
        if (usageCount > 0) {
            log.error("[1406] Cannot delete brand id={} - {} products depend on it", id, usageCount);
            throw new ConflictException(
                    "This brand is currently used by " + usageCount + " products and cannot be deleted. Deactivate it instead.");
        }
        repository.delete(brand);
    }

    private Brand find(Long id) {
        return repository.findById(id).orElseThrow(() -> {
            log.error("[1407] Brand not found id={}", id);
            return new NotFoundException("Brand not found: " + id);
        });
    }

    private boolean matchesQuery(Brand brand, String q) {
        return !StringUtils.hasText(q) || brand.getName().toLowerCase().contains(q.toLowerCase());
    }

    private void apply(Brand brand, BrandAdminRequest request) {
        brand.setName(request.name());
        brand.setDisplayOrder(request.displayOrder() != null ? request.displayOrder() : 0);
        brand.setActive(request.active());
    }

    private BrandAdminResponse toResponse(Brand b, Map<Long, String> names) {
        return new BrandAdminResponse(
                b.getId(),
                b.getName(),
                b.getDisplayOrder(),
                b.isActive(),
                names.get(b.getCreatedBy()),
                names.get(b.getUpdatedBy()),
                b.getCreatedAt(),
                b.getUpdatedAt());
    }
}
