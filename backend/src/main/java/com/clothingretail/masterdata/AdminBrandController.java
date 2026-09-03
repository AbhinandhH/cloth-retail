package com.clothingretail.masterdata;

import com.clothingretail.common.ConflictException;
import com.clothingretail.common.NotFoundException;
import com.clothingretail.masterdata.dto.BrandAdminRequest;
import com.clothingretail.masterdata.dto.BrandAdminResponse;
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
@RequestMapping("/api/admin/brands")
@PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
public class AdminBrandController {

    private final BrandRepository repository;

    public AdminBrandController(BrandRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<BrandAdminResponse> list() {
        return repository.findAll().stream().map(this::toResponse).toList();
    }

    @GetMapping("/{id}")
    public BrandAdminResponse get(@PathVariable Long id) {
        return toResponse(find(id));
    }

    @PostMapping
    public ResponseEntity<BrandAdminResponse> create(@Valid @RequestBody BrandAdminRequest request) {
        if (repository.existsByNameIgnoreCase(request.name())) {
            throw new ConflictException("A brand with this name already exists");
        }
        Brand brand = new Brand();
        apply(brand, request);
        return ResponseEntity.ok(toResponse(repository.save(brand)));
    }

    @PutMapping("/{id}")
    public BrandAdminResponse update(@PathVariable Long id, @Valid @RequestBody BrandAdminRequest request) {
        Brand brand = find(id);
        apply(brand, request);
        return toResponse(repository.save(brand));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        repository.delete(find(id));
        return ResponseEntity.noContent().build();
    }

    private Brand find(Long id) {
        return repository.findById(id).orElseThrow(() -> new NotFoundException("Brand not found: " + id));
    }

    private void apply(Brand brand, BrandAdminRequest request) {
        brand.setName(request.name());
        brand.setDisplayOrder(request.displayOrder() != null ? request.displayOrder() : 0);
        brand.setActive(request.active());
    }

    private BrandAdminResponse toResponse(Brand b) {
        return new BrandAdminResponse(b.getId(), b.getName(), b.getDisplayOrder(), b.isActive());
    }
}
