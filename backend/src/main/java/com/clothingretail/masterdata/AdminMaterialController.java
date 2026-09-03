package com.clothingretail.masterdata;

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

    private final MaterialService materialService;

    public AdminMaterialController(MaterialService materialService) {
        this.materialService = materialService;
    }

    @GetMapping
    public List<MaterialAdminResponse> list() {
        return materialService.listAdmin();
    }

    @GetMapping("/{id}")
    public MaterialAdminResponse get(@PathVariable Long id) {
        return materialService.getAdmin(id);
    }

    @PostMapping
    public ResponseEntity<MaterialAdminResponse> create(@Valid @RequestBody MaterialAdminRequest request) {
        return ResponseEntity.ok(materialService.create(request));
    }

    @PutMapping("/{id}")
    public MaterialAdminResponse update(@PathVariable Long id, @Valid @RequestBody MaterialAdminRequest request) {
        return materialService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        materialService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
