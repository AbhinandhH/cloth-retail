package com.clothingretail.masterdata;

import com.clothingretail.common.ConflictException;
import com.clothingretail.common.NotFoundException;
import com.clothingretail.masterdata.dto.ColorAdminRequest;
import com.clothingretail.masterdata.dto.ColorAdminResponse;
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
@RequestMapping("/api/admin/colors")
@PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
public class AdminColorController {

    private final ColorRepository repository;

    public AdminColorController(ColorRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<ColorAdminResponse> list() {
        return repository.findAll().stream().map(this::toResponse).toList();
    }

    @GetMapping("/{id}")
    public ColorAdminResponse get(@PathVariable Long id) {
        return toResponse(find(id));
    }

    @PostMapping
    public ResponseEntity<ColorAdminResponse> create(@Valid @RequestBody ColorAdminRequest request) {
        if (repository.existsByNameIgnoreCase(request.name())) {
            throw new ConflictException("A color with this name already exists");
        }
        Color color = new Color();
        apply(color, request);
        return ResponseEntity.ok(toResponse(repository.save(color)));
    }

    @PutMapping("/{id}")
    public ColorAdminResponse update(@PathVariable Long id, @Valid @RequestBody ColorAdminRequest request) {
        Color color = find(id);
        apply(color, request);
        return toResponse(repository.save(color));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        repository.delete(find(id));
        return ResponseEntity.noContent().build();
    }

    private Color find(Long id) {
        return repository.findById(id).orElseThrow(() -> new NotFoundException("Color not found: " + id));
    }

    private void apply(Color color, ColorAdminRequest request) {
        color.setName(request.name());
        color.setHexCode(request.hexCode());
        color.setDisplayOrder(request.displayOrder() != null ? request.displayOrder() : 0);
        color.setActive(request.active());
    }

    private ColorAdminResponse toResponse(Color c) {
        return new ColorAdminResponse(c.getId(), c.getName(), c.getHexCode(), c.getDisplayOrder(), c.isActive());
    }
}
