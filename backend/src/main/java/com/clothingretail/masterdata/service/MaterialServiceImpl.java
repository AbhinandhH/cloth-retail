package com.clothingretail.masterdata.service;

import com.clothingretail.common.AuditorNameResolver;
import com.clothingretail.common.ConflictException;
import com.clothingretail.common.NotFoundException;
import com.clothingretail.masterdata.Material;
import com.clothingretail.masterdata.dto.MaterialAdminRequest;
import com.clothingretail.masterdata.dto.MaterialAdminResponse;
import com.clothingretail.masterdata.repository.MaterialRepository;
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
public class MaterialServiceImpl implements MaterialService {

    private final MaterialRepository repository;
    private final ProductRepository productRepository;
    private final AuditorNameResolver auditorNameResolver;

    public MaterialServiceImpl(MaterialRepository repository, ProductRepository productRepository, AuditorNameResolver auditorNameResolver) {
        this.repository = repository;
        this.productRepository = productRepository;
        this.auditorNameResolver = auditorNameResolver;
    }

    @Override
    public List<MaterialAdminResponse> listAdmin(String q, Boolean active) {
        log.info("[1427] Listing materials query={} active={}", q, active);
        List<Material> materials = repository.findAll().stream()
                .filter(m -> matchesQuery(m, q))
                .filter(m -> active == null || m.isActive() == active)
                .toList();
        Map<Long, String> names = auditorNameResolver.resolveNames(
                materials.stream().flatMap(m -> Stream.of(m.getCreatedBy(), m.getUpdatedBy())).toList());
        return materials.stream().map(m -> toResponse(m, names)).toList();
    }

    @Override
    public MaterialAdminResponse getAdmin(Long id) {
        log.info("[1428] Fetching material id={}", id);
        Material material = find(id);
        return toResponse(material, auditorNameResolver.resolveNames(material.getCreatedBy(), material.getUpdatedBy()));
    }

    @Override
    @Transactional
    public MaterialAdminResponse create(MaterialAdminRequest request) {
        log.info("[1429] Creating material name={} displayOrder={} active={}", request.name(), request.displayOrder(), request.active());
        if (repository.existsByNameIgnoreCase(request.name())) {
            log.error("[1430] Cannot create material - name={} already exists", request.name());
            throw new ConflictException("A material with this name already exists");
        }
        Material material = new Material();
        apply(material, request);
        Material saved = repository.save(material);
        return toResponse(saved, auditorNameResolver.resolveNames(saved.getCreatedBy(), saved.getUpdatedBy()));
    }

    @Override
    @Transactional
    public MaterialAdminResponse update(Long id, MaterialAdminRequest request) {
        log.info("[1431] Updating material id={} name={} displayOrder={} active={}", id, request.name(), request.displayOrder(), request.active());
        Material material = find(id);
        apply(material, request);
        Material saved = repository.save(material);
        return toResponse(saved, auditorNameResolver.resolveNames(saved.getCreatedBy(), saved.getUpdatedBy()));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        log.info("[1432] Deleting material id={}", id);
        Material material = find(id);
        long usageCount = productRepository.countByMaterialId(id);
        if (usageCount > 0) {
            log.error("[1433] Cannot delete material id={} - {} products depend on it", id, usageCount);
            throw new ConflictException(
                    "This material is currently used by " + usageCount + " products and cannot be deleted. Deactivate it instead.");
        }
        repository.delete(material);
    }

    private Material find(Long id) {
        return repository.findById(id).orElseThrow(() -> {
            log.error("[1434] Material not found id={}", id);
            return new NotFoundException("Material not found: " + id);
        });
    }

    private boolean matchesQuery(Material material, String q) {
        return !StringUtils.hasText(q) || material.getName().toLowerCase().contains(q.toLowerCase());
    }

    private void apply(Material material, MaterialAdminRequest request) {
        material.setName(request.name());
        material.setDisplayOrder(request.displayOrder() != null ? request.displayOrder() : 0);
        material.setActive(request.active());
    }

    private MaterialAdminResponse toResponse(Material m, Map<Long, String> names) {
        return new MaterialAdminResponse(
                m.getId(),
                m.getName(),
                m.getDisplayOrder(),
                m.isActive(),
                names.get(m.getCreatedBy()),
                names.get(m.getUpdatedBy()),
                m.getCreatedAt(),
                m.getUpdatedAt());
    }
}
