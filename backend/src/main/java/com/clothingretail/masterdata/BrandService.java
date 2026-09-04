package com.clothingretail.masterdata;

import com.clothingretail.common.AuditorNameResolver;
import com.clothingretail.common.ConflictException;
import com.clothingretail.common.NotFoundException;
import com.clothingretail.masterdata.dto.BrandAdminRequest;
import com.clothingretail.masterdata.dto.BrandAdminResponse;
import com.clothingretail.product.ProductRepository;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Transactional(readOnly = true)
public class BrandService {

    private final BrandRepository repository;
    private final ProductRepository productRepository;
    private final AuditorNameResolver auditorNameResolver;

    public BrandService(BrandRepository repository, ProductRepository productRepository, AuditorNameResolver auditorNameResolver) {
        this.repository = repository;
        this.productRepository = productRepository;
        this.auditorNameResolver = auditorNameResolver;
    }

    public List<BrandAdminResponse> listAdmin(String q, Boolean active) {
        List<Brand> brands = repository.findAll().stream()
                .filter(b -> matchesQuery(b, q))
                .filter(b -> active == null || b.isActive() == active)
                .toList();
        Map<Long, String> names = auditorNameResolver.resolveNames(
                brands.stream().flatMap(b -> Stream.of(b.getCreatedBy(), b.getUpdatedBy())).toList());
        return brands.stream().map(b -> toResponse(b, names)).toList();
    }

    public BrandAdminResponse getAdmin(Long id) {
        Brand brand = find(id);
        return toResponse(brand, auditorNameResolver.resolveNames(brand.getCreatedBy(), brand.getUpdatedBy()));
    }

    @Transactional
    public BrandAdminResponse create(BrandAdminRequest request) {
        if (repository.existsByNameIgnoreCase(request.name())) {
            throw new ConflictException("A brand with this name already exists");
        }
        Brand brand = new Brand();
        apply(brand, request);
        Brand saved = repository.save(brand);
        return toResponse(saved, auditorNameResolver.resolveNames(saved.getCreatedBy(), saved.getUpdatedBy()));
    }

    @Transactional
    public BrandAdminResponse update(Long id, BrandAdminRequest request) {
        Brand brand = find(id);
        apply(brand, request);
        Brand saved = repository.save(brand);
        return toResponse(saved, auditorNameResolver.resolveNames(saved.getCreatedBy(), saved.getUpdatedBy()));
    }

    @Transactional
    public void delete(Long id) {
        Brand brand = find(id);
        long usageCount = productRepository.countByBrandId(id);
        if (usageCount > 0) {
            throw new ConflictException(
                    "This brand is currently used by " + usageCount + " products and cannot be deleted. Deactivate it instead.");
        }
        repository.delete(brand);
    }

    private Brand find(Long id) {
        return repository.findById(id).orElseThrow(() -> new NotFoundException("Brand not found: " + id));
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
