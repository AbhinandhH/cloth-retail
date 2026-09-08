package com.clothingretail.dashboard;

import com.clothingretail.dashboard.dto.AdminDashboardResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/dashboard")
@PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
public class AdminDashboardController {

    private final AdminDashboardQueryService adminDashboardQueryService;

    public AdminDashboardController(AdminDashboardQueryService adminDashboardQueryService) {
        this.adminDashboardQueryService = adminDashboardQueryService;
    }

    @GetMapping
    public AdminDashboardResponse dashboard() {
        return adminDashboardQueryService.dashboard();
    }
}
