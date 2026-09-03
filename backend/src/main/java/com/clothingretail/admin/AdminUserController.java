package com.clothingretail.admin;

import com.clothingretail.auth.AuthService;
import com.clothingretail.auth.dto.CreateAdminRequest;
import com.clothingretail.auth.dto.UserSummary;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** SUPER_ADMIN-only endpoint for creating additional ADMIN accounts. No public admin sign-up exists. */
@RestController
@RequestMapping("/api/admin/admins")
public class AdminUserController {

    private final AuthService authService;

    public AdminUserController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<UserSummary> createAdmin(@Valid @RequestBody CreateAdminRequest request) {
        return ResponseEntity.ok(authService.createAdmin(request));
    }
}
