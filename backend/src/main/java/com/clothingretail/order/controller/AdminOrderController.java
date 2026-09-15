package com.clothingretail.order.controller;

import com.clothingretail.common.PageResponse;
import com.clothingretail.order.OrderStatus;
import com.clothingretail.order.dto.AdminOrderCancelRequest;
import com.clothingretail.order.dto.AdminOrderDashboardResponse;
import com.clothingretail.order.dto.AdminOrderDetailResponse;
import com.clothingretail.order.dto.AdminOrderNoteRequest;
import com.clothingretail.order.dto.AdminOrderNoteResponse;
import com.clothingretail.order.dto.AdminOrderRow;
import com.clothingretail.order.dto.AdminOrderStatusUpdateRequest;
import com.clothingretail.order.dto.AdminRefundRequest;
import com.clothingretail.order.dto.AdminRefundResponse;
import com.clothingretail.order.dto.AdminShipmentRequest;
import com.clothingretail.order.dto.AdminShipmentResponse;
import com.clothingretail.order.service.AdminOrderQueryService;
import com.clothingretail.order.service.AdminOrderService;
import com.clothingretail.payment.PaymentStatus;
import jakarta.validation.Valid;
import java.time.Instant;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/orders")
@PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
public class AdminOrderController {

    private final AdminOrderQueryService adminOrderQueryService;
    private final AdminOrderService adminOrderService;

    public AdminOrderController(AdminOrderQueryService adminOrderQueryService, AdminOrderService adminOrderService) {
        this.adminOrderQueryService = adminOrderQueryService;
        this.adminOrderService = adminOrderService;
    }

    @GetMapping
    public PageResponse<AdminOrderRow> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) OrderStatus orderStatus,
            @RequestParam(required = false) PaymentStatus paymentStatus,
            @RequestParam(required = false) Instant dateFrom,
            @RequestParam(required = false) Instant dateTo,
            @RequestParam(required = false) String paymentMethod,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) String dir) {
        return PageResponse.of(adminOrderQueryService.list(
                q, orderStatus, paymentStatus, dateFrom, dateTo, paymentMethod, sort, dir, page, size));
    }

    @GetMapping("/dashboard")
    public AdminOrderDashboardResponse dashboard() {
        return adminOrderQueryService.dashboard();
    }

    @GetMapping("/{id}")
    public AdminOrderDetailResponse detail(@PathVariable Long id) {
        return adminOrderQueryService.detail(id);
    }

    @PostMapping("/{id}/status")
    public AdminOrderDetailResponse updateStatus(
            @PathVariable Long id, @Valid @RequestBody AdminOrderStatusUpdateRequest request, Authentication authentication) {
        return adminOrderService.updateStatus(id, request, actingUserId(authentication));
    }

    @PostMapping("/{id}/cancel")
    public AdminOrderDetailResponse cancel(
            @PathVariable Long id, @Valid @RequestBody AdminOrderCancelRequest request, Authentication authentication) {
        return adminOrderService.cancel(id, request, actingUserId(authentication));
    }

    @PostMapping("/{id}/notes")
    public AdminOrderNoteResponse addNote(
            @PathVariable Long id, @Valid @RequestBody AdminOrderNoteRequest request, Authentication authentication) {
        return adminOrderService.addNote(id, request, actingUserId(authentication));
    }

    @PutMapping("/{id}/shipment")
    public AdminShipmentResponse upsertShipment(@PathVariable Long id, @Valid @RequestBody AdminShipmentRequest request) {
        return adminOrderService.upsertShipment(id, request);
    }

    @PostMapping("/{id}/refund")
    public AdminRefundResponse refund(
            @PathVariable Long id, @Valid @RequestBody AdminRefundRequest request, Authentication authentication) {
        return adminOrderService.refund(id, request, actingUserId(authentication));
    }

    private Long actingUserId(Authentication authentication) {
        return authentication != null ? (Long) authentication.getPrincipal() : null;
    }
}
