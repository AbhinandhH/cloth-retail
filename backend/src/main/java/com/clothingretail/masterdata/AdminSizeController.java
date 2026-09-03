package com.clothingretail.masterdata;

import com.clothingretail.common.ConflictException;
import com.clothingretail.common.NotFoundException;
import com.clothingretail.masterdata.dto.SizeAdminRequest;
import com.clothingretail.masterdata.dto.SizeAdminResponse;
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
@RequestMapping("/api/admin/sizes")
@PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
public class AdminSizeController {

    private final SizeRepository repository;

    public AdminSizeController(SizeRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<SizeAdminResponse> list() {
        return repository.findAll().stream().map(this::toResponse).toList();
    }

    @GetMapping("/{id}")
    public SizeAdminResponse get(@PathVariable Long id) {
        return toResponse(find(id));
    }

    @PostMapping
    public ResponseEntity<SizeAdminResponse> create(@Valid @RequestBody SizeAdminRequest request) {
        if (repository.existsByNameIgnoreCase(request.name())) {
            throw new ConflictException("A size with this name already exists");
        }
        Size size = new Size();
        apply(size, request);
        return ResponseEntity.ok(toResponse(repository.save(size)));
    }

    @PutMapping("/{id}")
    public SizeAdminResponse update(@PathVariable Long id, @Valid @RequestBody SizeAdminRequest request) {
        Size size = find(id);
        apply(size, request);
        return toResponse(repository.save(size));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        repository.delete(find(id));
        return ResponseEntity.noContent().build();
    }

    private Size find(Long id) {
        return repository.findById(id).orElseThrow(() -> new NotFoundException("Size not found: " + id));
    }

    private void apply(Size size, SizeAdminRequest request) {
        size.setName(request.name());
        size.setDisplayOrder(request.displayOrder() != null ? request.displayOrder() : 0);
        size.setActive(request.active());
    }

    private SizeAdminResponse toResponse(Size s) {
        return new SizeAdminResponse(s.getId(), s.getName(), s.getDisplayOrder(), s.isActive());
    }
}
