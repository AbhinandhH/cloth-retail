package com.clothingretail.masterdata;

import com.clothingretail.common.ConflictException;
import com.clothingretail.common.NotFoundException;
import com.clothingretail.masterdata.dto.MaterialAdminRequest;
import com.clothingretail.masterdata.dto.MaterialAdminResponse;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class MaterialService {

    private final MaterialRepository repository;

    public MaterialService(MaterialRepository repository) {
        this.repository = repository;
    }

    public List<MaterialAdminResponse> listAdmin() {
        return repository.findAll().stream().map(this::toResponse).toList();
    }

    public MaterialAdminResponse getAdmin(Long id) {
        return toResponse(find(id));
    }

    @Transactional
    public MaterialAdminResponse create(MaterialAdminRequest request) {
        if (repository.existsByNameIgnoreCase(request.name())) {
            throw new ConflictException("A material with this name already exists");
        }
        Material material = new Material();
        apply(material, request);
        return toResponse(repository.save(material));
    }

    @Transactional
    public MaterialAdminResponse update(Long id, MaterialAdminRequest request) {
        Material material = find(id);
        apply(material, request);
        return toResponse(repository.save(material));
    }

    @Transactional
    public void delete(Long id) {
        repository.delete(find(id));
    }

    private Material find(Long id) {
        return repository.findById(id).orElseThrow(() -> new NotFoundException("Material not found: " + id));
    }

    private void apply(Material material, MaterialAdminRequest request) {
        material.setName(request.name());
        material.setDisplayOrder(request.displayOrder() != null ? request.displayOrder() : 0);
        material.setActive(request.active());
    }

    private MaterialAdminResponse toResponse(Material m) {
        return new MaterialAdminResponse(m.getId(), m.getName(), m.getDisplayOrder(), m.isActive());
    }
}
