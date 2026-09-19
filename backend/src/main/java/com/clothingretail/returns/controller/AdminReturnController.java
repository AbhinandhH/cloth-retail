package com.clothingretail.returns.controller;

import com.clothingretail.common.PageResponse;
import com.clothingretail.returns.RequestType;
import com.clothingretail.returns.ReturnRequestStatus;
import com.clothingretail.returns.dto.AdminInitiateRefundRequest;
import com.clothingretail.returns.dto.AdminReturnDecisionRequest;
import com.clothingretail.returns.dto.AdminReturnDetailResponse;
import com.clothingretail.returns.dto.AdminReturnRow;
import com.clothingretail.returns.service.EvidenceFile;
import com.clothingretail.returns.service.ReturnAdminService;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** Admin return/exchange/damage-claim review, evidence upload, and refund-initiation endpoints. Same ADMIN-or-SUPER_ADMIN operational tier as AdminOrderController. */
@RestController
@RequestMapping("/api/admin/returns")
@PreAuthorize("hasRole('ADMIN') or (hasRole('EMPLOYEE') and @modulePermission.hasAccess('RETURNS'))")
public class AdminReturnController {

    private final ReturnAdminService returnAdminService;

    public AdminReturnController(ReturnAdminService returnAdminService) {
        this.returnAdminService = returnAdminService;
    }

    @GetMapping
    public PageResponse<AdminReturnRow> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) RequestType requestType,
            @RequestParam(required = false) ReturnRequestStatus status,
            @RequestParam(required = false) String q) {
        return returnAdminService.list(requestType, status, q, page, size);
    }

    @GetMapping("/{id}")
    public AdminReturnDetailResponse detail(@PathVariable Long id) {
        return returnAdminService.detail(id);
    }

    @PostMapping("/{id}/approve-exchange")
    public AdminReturnDetailResponse approveExchange(
            @PathVariable Long id, @Valid @RequestBody AdminReturnDecisionRequest request, Authentication authentication) {
        return returnAdminService.approveExchange(id, request, actingUserId(authentication));
    }

    @PostMapping("/{id}/approve-damage")
    public AdminReturnDetailResponse approveDamageClaim(
            @PathVariable Long id, @Valid @RequestBody AdminReturnDecisionRequest request, Authentication authentication) {
        return returnAdminService.approveDamageClaim(id, request, actingUserId(authentication));
    }

    @PostMapping("/{id}/reject")
    public AdminReturnDetailResponse reject(
            @PathVariable Long id, @Valid @RequestBody AdminReturnDecisionRequest request, Authentication authentication) {
        return returnAdminService.reject(id, request, actingUserId(authentication));
    }

    @PostMapping("/{id}/evidence")
    public AdminReturnDetailResponse uploadEvidence(@PathVariable Long id, @RequestParam("file") MultipartFile file) {
        return returnAdminService.uploadEvidence(id, file);
    }

    @GetMapping("/{id}/evidence")
    public ResponseEntity<Resource> evidence(@PathVariable Long id) {
        EvidenceFile file = returnAdminService.loadEvidence(id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(file.contentType()))
                .body(file.resource());
    }

    @PostMapping("/{id}/refund")
    public AdminReturnDetailResponse refund(
            @PathVariable Long id, @Valid @RequestBody AdminInitiateRefundRequest request, Authentication authentication) {
        return returnAdminService.initiateRefund(id, request, actingUserId(authentication));
    }

    private Long actingUserId(Authentication authentication) {
        return authentication != null ? (Long) authentication.getPrincipal() : null;
    }
}
