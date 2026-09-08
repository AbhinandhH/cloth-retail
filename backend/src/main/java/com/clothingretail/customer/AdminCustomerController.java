package com.clothingretail.customer;

import com.clothingretail.common.PageResponse;
import com.clothingretail.customer.dto.AdminCustomerDetailResponse;
import com.clothingretail.customer.dto.AdminCustomerRow;
import com.clothingretail.customer.dto.AdminCustomerStatusUpdateRequest;
import com.clothingretail.order.dto.AdminOrderRow;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/customers")
@PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
public class AdminCustomerController {

    private final AdminCustomerQueryService adminCustomerQueryService;
    private final AdminCustomerService adminCustomerService;

    public AdminCustomerController(AdminCustomerQueryService adminCustomerQueryService, AdminCustomerService adminCustomerService) {
        this.adminCustomerQueryService = adminCustomerQueryService;
        this.adminCustomerService = adminCustomerService;
    }

    @GetMapping
    public PageResponse<AdminCustomerRow> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Boolean enabled,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) String dir) {
        return PageResponse.of(adminCustomerQueryService.list(q, enabled, sort, dir, page, size));
    }

    @GetMapping("/{id}")
    public AdminCustomerDetailResponse detail(@PathVariable Long id) {
        return adminCustomerQueryService.detail(id);
    }

    @GetMapping("/{id}/orders")
    public PageResponse<AdminOrderRow> orderHistory(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return PageResponse.of(adminCustomerQueryService.orderHistory(id, page, size));
    }

    @PatchMapping("/{id}/status")
    public AdminCustomerDetailResponse updateStatus(
            @PathVariable Long id, @Valid @RequestBody AdminCustomerStatusUpdateRequest request) {
        return adminCustomerService.setEnabled(id, request.enabled());
    }
}
