package com.clothingretail.inventory.controller;

import com.clothingretail.inventory.dto.DamageReasonAdminRequest;
import com.clothingretail.inventory.dto.DamageReasonAdminResponse;
import com.clothingretail.inventory.service.DamageReasonService;
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
@RequestMapping("/api/admin/damage-reasons")
@PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
public class AdminDamageReasonController {

    private final DamageReasonService damageReasonService;

    public AdminDamageReasonController(DamageReasonService damageReasonService) {
        this.damageReasonService = damageReasonService;
    }

    @GetMapping
    public List<DamageReasonAdminResponse> list(
            @RequestParam(required = false) String q, @RequestParam(required = false) Boolean active) {
        return damageReasonService.listAdmin(q, active);
    }

    @GetMapping("/{id}")
    public DamageReasonAdminResponse get(@PathVariable Long id) {
        return damageReasonService.getAdmin(id);
    }

    @PostMapping
    public ResponseEntity<DamageReasonAdminResponse> create(@Valid @RequestBody DamageReasonAdminRequest request) {
        return ResponseEntity.ok(damageReasonService.create(request));
    }

    @PutMapping("/{id}")
    public DamageReasonAdminResponse update(@PathVariable Long id, @Valid @RequestBody DamageReasonAdminRequest request) {
        return damageReasonService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        damageReasonService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
