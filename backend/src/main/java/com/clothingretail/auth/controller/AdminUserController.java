package com.clothingretail.auth.controller;

import com.clothingretail.auth.dto.AdminStaffRow;
import com.clothingretail.auth.dto.CreateAdminRequest;
import com.clothingretail.auth.dto.ModulePermissionRow;
import com.clothingretail.auth.dto.UpdatePermissionsRequest;
import com.clothingretail.auth.dto.UpdateStaffStatusRequest;
import com.clothingretail.auth.dto.UserSummary;
import com.clothingretail.auth.service.AuthService;
import com.clothingretail.auth.service.ModulePermissionService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Staff management: SUPER_ADMIN (the software owner) creates a store's first ADMIN account;
 * from then on any ADMIN can create further ADMIN/EMPLOYEE accounts, disable one, or edit its
 * per-module permissions. Never touches SUPER_ADMIN accounts themselves - that role is exclusively
 * provisioned via AdminBootstrapRunner, never through this controller.
 */
@RestController
@RequestMapping("/api/admin/admins")
public class AdminUserController {

    private final AuthService authService;
    private final ModulePermissionService modulePermissionService;

    public AdminUserController(AuthService authService, ModulePermissionService modulePermissionService) {
        this.authService = authService;
        this.modulePermissionService = modulePermissionService;
    }

    @PostMapping
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('ADMIN')")
    public ResponseEntity<UserSummary> createAdmin(@Valid @RequestBody CreateAdminRequest request) {
        return ResponseEntity.ok(authService.createAdmin(request));
    }

    @GetMapping
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('ADMIN')")
    public List<AdminStaffRow> list() {
        return authService.listStaff();
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> setStatus(@PathVariable Long id, @Valid @RequestBody UpdateStaffStatusRequest request) {
        authService.setStaffStatus(id, request.enabled());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/permissions")
    @PreAuthorize("hasRole('ADMIN')")
    public List<ModulePermissionRow> getPermissions(@PathVariable Long id) {
        return modulePermissionService.listForUser(id);
    }

    @PutMapping("/{id}/permissions")
    @PreAuthorize("hasRole('ADMIN')")
    public List<ModulePermissionRow> updatePermissions(@PathVariable Long id, @Valid @RequestBody UpdatePermissionsRequest request) {
        modulePermissionService.replaceForUser(id, request.grants());
        return modulePermissionService.listForUser(id);
    }
}
