package com.clothingretail.masterdata;

import com.clothingretail.common.ConflictException;
import com.clothingretail.common.NotFoundException;
import com.clothingretail.masterdata.dto.VendorAdminRequest;
import com.clothingretail.masterdata.dto.VendorAdminResponse;
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
@RequestMapping("/api/admin/vendors")
@PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
public class AdminVendorController {

    private final VendorRepository repository;

    public AdminVendorController(VendorRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<VendorAdminResponse> list() {
        return repository.findAll().stream().map(this::toResponse).toList();
    }

    @GetMapping("/{id}")
    public VendorAdminResponse get(@PathVariable Long id) {
        return toResponse(find(id));
    }

    @PostMapping
    public ResponseEntity<VendorAdminResponse> create(@Valid @RequestBody VendorAdminRequest request) {
        if (repository.existsByNameIgnoreCase(request.name())) {
            throw new ConflictException("A vendor with this name already exists");
        }
        Vendor vendor = new Vendor();
        apply(vendor, request);
        return ResponseEntity.ok(toResponse(repository.save(vendor)));
    }

    @PutMapping("/{id}")
    public VendorAdminResponse update(@PathVariable Long id, @Valid @RequestBody VendorAdminRequest request) {
        Vendor vendor = find(id);
        apply(vendor, request);
        return toResponse(repository.save(vendor));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        repository.delete(find(id));
        return ResponseEntity.noContent().build();
    }

    private Vendor find(Long id) {
        return repository.findById(id).orElseThrow(() -> new NotFoundException("Vendor not found: " + id));
    }

    private void apply(Vendor vendor, VendorAdminRequest request) {
        vendor.setName(request.name());
        vendor.setContactName(request.contactName());
        vendor.setContactEmail(request.contactEmail());
        vendor.setContactPhone(request.contactPhone());
        vendor.setActive(request.active());
    }

    private VendorAdminResponse toResponse(Vendor v) {
        return new VendorAdminResponse(v.getId(), v.getName(), v.getContactName(), v.getContactEmail(), v.getContactPhone(), v.isActive());
    }
}
