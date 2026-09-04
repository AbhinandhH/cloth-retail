package com.clothingretail.masterdata;

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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/vendors")
@PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
public class AdminVendorController {

    private final VendorService vendorService;

    public AdminVendorController(VendorService vendorService) {
        this.vendorService = vendorService;
    }

    @GetMapping
    public List<VendorAdminResponse> list(
            @RequestParam(required = false) String q, @RequestParam(required = false) Boolean active) {
        return vendorService.listAdmin(q, active);
    }

    @GetMapping("/{id}")
    public VendorAdminResponse get(@PathVariable Long id) {
        return vendorService.getAdmin(id);
    }

    @PostMapping
    public ResponseEntity<VendorAdminResponse> create(@Valid @RequestBody VendorAdminRequest request) {
        return ResponseEntity.ok(vendorService.create(request));
    }

    @PutMapping("/{id}")
    public VendorAdminResponse update(@PathVariable Long id, @Valid @RequestBody VendorAdminRequest request) {
        return vendorService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        vendorService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
