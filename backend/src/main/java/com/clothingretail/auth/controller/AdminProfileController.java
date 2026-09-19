package com.clothingretail.auth.controller;

import com.clothingretail.auth.dto.ModulePermissionRow;
import com.clothingretail.auth.dto.UpdateProfileRequest;
import com.clothingretail.auth.dto.UserSummary;
import com.clothingretail.auth.service.AuthService;
import com.clothingretail.auth.service.ModulePermissionService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Self-service for the currently authenticated admin-side account (SUPER_ADMIN, ADMIN, or
 * EMPLOYEE) - own profile edit/password change, and own module permissions (so the frontend can
 * gate AdminHome's tiles without needing staff-management rights). Every admin-side role may hit
 * this controller for their own account; nobody can use it to touch anyone else's.
 */
@RestController
@RequestMapping("/api/admin/profile")
@PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('ADMIN') or hasRole('EMPLOYEE')")
public class AdminProfileController {

    private final AuthService authService;
    private final ModulePermissionService modulePermissionService;

    public AdminProfileController(AuthService authService, ModulePermissionService modulePermissionService) {
        this.authService = authService;
        this.modulePermissionService = modulePermissionService;
    }

    @GetMapping
    public UserSummary get(Authentication authentication) {
        return authService.getCurrentUser(userId(authentication));
    }

    @PutMapping
    public UserSummary update(Authentication authentication, @Valid @RequestBody UpdateProfileRequest request) {
        return authService.updateProfile(userId(authentication), request);
    }

    @GetMapping("/permissions")
    public List<ModulePermissionRow> permissions(Authentication authentication) {
        return modulePermissionService.listForUser(userId(authentication));
    }

    private Long userId(Authentication authentication) {
        return (Long) authentication.getPrincipal();
    }
}
