package com.clothingretail.masterdata;

import com.clothingretail.common.ConflictException;
import com.clothingretail.common.NotFoundException;
import com.clothingretail.masterdata.dto.MaterialAdminRequest;
import com.clothingretail.masterdata.dto.MaterialAdminResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/materials")
@PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
public class AdminMaterialController {

    private final MaterialRepository repository;

    public AdminMaterialController(MaterialRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<MaterialAdminResponse> list() {
        return repository.findAll().stream().map(this::toResponse).toList();
    }

    @GetMapping("/{id}")
    public MaterialAdminResponse get(@PathVariable Long id) {
        return toResponse(find(id));
    }

    @PostMapping
    public ResponseEntity<MaterialAdminResponse> create(@Valid @RequestBody MaterialAdminRequest request) {
        if (repository.existsByNameIgnoreCase(request.name())) {
            throw new ConflictException("A material with this name already exists");
        }
        Material material = new Material();
        apply(material, request);
        return ResponseEntity.ok(toResponse(repository.save(material)));
    }

    @PutMapping("/{id}")
    public MaterialAdminResponse update(@PathVariable Long id, @Valid @RequestBody MaterialAdminRequest request) {
        Material material = find(id);
        apply(material, request);
        return toResponse(repository.save(material));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        repository.delete(find(id));
        return ResponseEntity.noContent().build();
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
