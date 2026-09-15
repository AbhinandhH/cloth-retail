package com.clothingretail.masterdata.service;

import com.clothingretail.common.AuditorNameResolver;
import com.clothingretail.common.ConflictException;
import com.clothingretail.common.NotFoundException;
import com.clothingretail.config.CacheConfig;
import com.clothingretail.masterdata.Size;
import com.clothingretail.masterdata.dto.SizeAdminRequest;
import com.clothingretail.masterdata.dto.SizeAdminResponse;
import com.clothingretail.masterdata.dto.SizeResponse;
import com.clothingretail.masterdata.repository.SizeRepository;
import com.clothingretail.product.repository.ProductVariantRepository;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import lombok.extern.log4j.Log4j2;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Transactional(readOnly = true)
@Log4j2
public class SizeServiceImpl implements SizeService {

    private final SizeRepository repository;
    private final ProductVariantRepository productVariantRepository;
    private final AuditorNameResolver auditorNameResolver;

    public SizeServiceImpl(
            SizeRepository repository, ProductVariantRepository productVariantRepository, AuditorNameResolver auditorNameResolver) {
        this.repository = repository;
        this.productVariantRepository = productVariantRepository;
        this.auditorNameResolver = auditorNameResolver;
    }

    @Override
    public List<SizeAdminResponse> listAdmin(String q, Boolean active) {
        log.info("[1448] Listing sizes query={} active={}", q, active);
        List<Size> sizes = repository.findAll().stream()
                .filter(s -> matchesQuery(s, q))
                .filter(s -> active == null || s.isActive() == active)
                .toList();
        Map<Long, String> names = auditorNameResolver.resolveNames(
                sizes.stream().flatMap(s -> Stream.of(s.getCreatedBy(), s.getUpdatedBy())).toList());
        return sizes.stream().map(s -> toResponse(s, names)).toList();
    }

    @Override
    public SizeAdminResponse getAdmin(Long id) {
        log.info("[1449] Fetching size id={}", id);
        Size size = find(id);
        return toResponse(size, auditorNameResolver.resolveNames(size.getCreatedBy(), size.getUpdatedBy()));
    }

    @Override
    @Cacheable(CacheConfig.SIZES_PUBLIC)
    public List<SizeResponse> listPublic() {
        log.info("[1450] Listing public sizes");
        return repository.findByActiveTrueOrderByDisplayOrderAscNameAsc().stream()
                .map(s -> new SizeResponse(s.getId(), s.getName()))
                .toList();
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = CacheConfig.SIZES_PUBLIC, allEntries = true)
    public SizeAdminResponse create(SizeAdminRequest request) {
        log.info("[1451] Creating size name={} displayOrder={} active={}", request.name(), request.displayOrder(), request.active());
        if (repository.existsByNameIgnoreCase(request.name())) {
            log.error("[1452] Cannot create size - name={} already exists", request.name());
            throw new ConflictException("A size with this name already exists");
        }
        Size size = new Size();
        apply(size, request);
        Size saved = repository.save(size);
        return toResponse(saved, auditorNameResolver.resolveNames(saved.getCreatedBy(), saved.getUpdatedBy()));
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = CacheConfig.SIZES_PUBLIC, allEntries = true)
    public SizeAdminResponse update(Long id, SizeAdminRequest request) {
        log.info("[1453] Updating size id={} name={} displayOrder={} active={}", id, request.name(), request.displayOrder(), request.active());
        Size size = find(id);
        apply(size, request);
        Size saved = repository.save(size);
        return toResponse(saved, auditorNameResolver.resolveNames(saved.getCreatedBy(), saved.getUpdatedBy()));
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = CacheConfig.SIZES_PUBLIC, allEntries = true)
    public void delete(Long id) {
        log.info("[1454] Deleting size id={}", id);
        Size size = find(id);
        long usageCount = productVariantRepository.countBySizeId(id);
        if (usageCount > 0) {
            log.error("[1455] Cannot delete size id={} - {} product variants depend on it", id, usageCount);
            throw new ConflictException(
                    "This size is currently used by " + usageCount + " product variants and cannot be deleted. Deactivate it instead.");
        }
        repository.delete(size);
    }

    private Size find(Long id) {
        return repository.findById(id).orElseThrow(() -> {
            log.error("[1456] Size not found id={}", id);
            return new NotFoundException("Size not found: " + id);
        });
    }

    private boolean matchesQuery(Size size, String q) {
        return !StringUtils.hasText(q) || size.getName().toLowerCase().contains(q.toLowerCase());
    }

    private void apply(Size size, SizeAdminRequest request) {
        size.setName(request.name());
        size.setDisplayOrder(request.displayOrder() != null ? request.displayOrder() : 0);
        size.setActive(request.active());
    }

    private SizeAdminResponse toResponse(Size s, Map<Long, String> names) {
        return new SizeAdminResponse(
                s.getId(),
                s.getName(),
                s.getDisplayOrder(),
                s.isActive(),
                names.get(s.getCreatedBy()),
                names.get(s.getUpdatedBy()),
                s.getCreatedAt(),
                s.getUpdatedAt());
    }
}
