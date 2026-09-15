package com.clothingretail.returns.controller;

import com.clothingretail.common.PageResponse;
import com.clothingretail.returns.dto.CreateDamageRequest;
import com.clothingretail.returns.dto.CreateExchangeRequest;
import com.clothingretail.returns.dto.EligibleOrderItemResponse;
import com.clothingretail.returns.dto.ReturnRequestDetailResponse;
import com.clothingretail.returns.dto.ReturnRequestSummaryResponse;
import com.clothingretail.returns.service.EvidenceFile;
import com.clothingretail.returns.service.ReturnRequestService;
import jakarta.validation.Valid;
import java.util.List;
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

/**
 * Customer-facing return/exchange/damage-report endpoints. Every method resolves ownership from
 * the authenticated principal only - see ReturnRequestServiceImpl's own doc comment.
 */
@RestController
@RequestMapping("/api/returns")
@PreAuthorize("hasRole('CUSTOMER')")
public class CustomerReturnController {

    private final ReturnRequestService returnRequestService;

    public CustomerReturnController(ReturnRequestService returnRequestService) {
        this.returnRequestService = returnRequestService;
    }

    @GetMapping("/orders/{orderId}/eligible-items")
    public List<EligibleOrderItemResponse> eligibleItems(@PathVariable Long orderId, Authentication authentication) {
        return returnRequestService.listEligibleItems(userId(authentication), orderId);
    }

    @PostMapping("/orders/{orderId}/items/{itemId}/exchange")
    public ReturnRequestDetailResponse requestExchange(
            @PathVariable Long orderId, @PathVariable Long itemId,
            @Valid @RequestBody CreateExchangeRequest request, Authentication authentication) {
        return returnRequestService.createExchangeRequest(userId(authentication), orderId, itemId, request);
    }

    @PostMapping("/orders/{orderId}/items/{itemId}/damage")
    public ReturnRequestDetailResponse reportDamage(
            @PathVariable Long orderId, @PathVariable Long itemId,
            @Valid @RequestBody CreateDamageRequest request, Authentication authentication) {
        return returnRequestService.createDamageRequest(userId(authentication), orderId, itemId, request);
    }

    @GetMapping
    public PageResponse<ReturnRequestSummaryResponse> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {
        return returnRequestService.listMyRequests(userId(authentication), page, size);
    }

    @GetMapping("/{id}")
    public ReturnRequestDetailResponse detail(@PathVariable Long id, Authentication authentication) {
        return returnRequestService.getMyRequest(userId(authentication), id);
    }

    @GetMapping("/{id}/evidence")
    public ResponseEntity<Resource> evidence(@PathVariable Long id, Authentication authentication) {
        EvidenceFile file = returnRequestService.loadMyEvidence(userId(authentication), id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(file.contentType()))
                .body(file.resource());
    }

    private Long userId(Authentication authentication) {
        return (Long) authentication.getPrincipal();
    }
}
