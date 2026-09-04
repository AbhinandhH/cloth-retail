package com.clothingretail.masterdata;

import com.clothingretail.common.AuditorNameResolver;
import com.clothingretail.common.ConflictException;
import com.clothingretail.common.NotFoundException;
import com.clothingretail.masterdata.dto.MaterialAdminRequest;
import com.clothingretail.masterdata.dto.MaterialAdminResponse;
import com.clothingretail.product.ProductRepository;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Transactional(readOnly = true)
public class MaterialService {

    private final MaterialRepository repository;
    private final ProductRepository productRepository;
    private final AuditorNameResolver auditorNameResolver;

    public MaterialService(MaterialRepository repository, ProductRepository productRepository, AuditorNameResolver auditorNameResolver) {
        this.repository = repository;
        this.productRepository = productRepository;
        this.auditorNameResolver = auditorNameResolver;
    }

    public List<MaterialAdminResponse> listAdmin(String q, Boolean active) {
        List<Material> materials = repository.findAll().stream()
                .filter(m -> matchesQuery(m, q))
                .filter(m -> active == null || m.isActive() == active)
                .toList();
        Map<Long, String> names = auditorNameResolver.resolveNames(
                materials.stream().flatMap(m -> Stream.of(m.getCreatedBy(), m.getUpdatedBy())).toList());
        return materials.stream().map(m -> toResponse(m, names)).toList();
    }

    public MaterialAdminResponse getAdmin(Long id) {
        Material material = find(id);
        return toResponse(material, auditorNameResolver.resolveNames(material.getCreatedBy(), material.getUpdatedBy()));
    }

    @Transactional
    public MaterialAdminResponse create(MaterialAdminRequest request) {
        if (repository.existsByNameIgnoreCase(request.name())) {
            throw new ConflictException("A material with this name already exists");
        }
        Material material = new Material();
        apply(material, request);
        Material saved = repository.save(material);
        return toResponse(saved, auditorNameResolver.resolveNames(saved.getCreatedBy(), saved.getUpdatedBy()));
    }

    @Transactional
    public MaterialAdminResponse update(Long id, MaterialAdminRequest request) {
        Material material = find(id);
        apply(material, request);
        Material saved = repository.save(material);
        return toResponse(saved, auditorNameResolver.resolveNames(saved.getCreatedBy(), saved.getUpdatedBy()));
    }

    @Transactional
    public void delete(Long id) {
        Material material = find(id);
        long usageCount = productRepository.countByMaterialId(id);
        if (usageCount > 0) {
            throw new ConflictException(
                    "This material is currently used by " + usageCount + " products and cannot be deleted. Deactivate it instead.");
        }
        repository.delete(material);
    }

    private Material find(Long id) {
        return repository.findById(id).orElseThrow(() -> new NotFoundException("Material not found: " + id));
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
