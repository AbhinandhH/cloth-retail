package com.clothingretail.inventory.controller;

import com.clothingretail.common.PageResponse;
import com.clothingretail.inventory.InventoryTransactionType;
import com.clothingretail.inventory.StockStatus;
import com.clothingretail.inventory.dto.DamageRecordRow;
import com.clothingretail.inventory.dto.DamageRequest;
import com.clothingretail.inventory.dto.DamageResponse;
import com.clothingretail.inventory.dto.DashboardResponse;
import com.clothingretail.inventory.dto.InventoryTransactionRow;
import com.clothingretail.inventory.dto.StockAdjustRequest;
import com.clothingretail.inventory.dto.StockAdjustResponse;
import com.clothingretail.inventory.dto.VariantInventoryRow;
import com.clothingretail.inventory.service.InventoryQueryService;
import com.clothingretail.inventory.service.StockService;
import com.clothingretail.product.ProductStatus;
import jakarta.validation.Valid;
import java.time.Instant;
import org.springframework.security.core.Authentication;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/inventory")
@PreAuthorize("hasRole('ADMIN') or (hasRole('EMPLOYEE') and @modulePermission.hasAccess('INVENTORY'))")
public class AdminInventoryController {

    private final StockService stockService;
    private final InventoryQueryService inventoryQueryService;

    public AdminInventoryController(StockService stockService, InventoryQueryService inventoryQueryService) {
        this.stockService = stockService;
        this.inventoryQueryService = inventoryQueryService;
    }

    @GetMapping("/variants")
    public PageResponse<VariantInventoryRow> listVariants(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long productId,
            @RequestParam(required = false) Long colorId,
            @RequestParam(required = false) Long sizeId,
            @RequestParam(required = false) StockStatus stockStatus,
            @RequestParam(required = false) ProductStatus productStatus,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) String dir) {
        return PageResponse.of(inventoryQueryService.listVariants(
                q, categoryId, productId, colorId, sizeId, stockStatus, productStatus, sort, dir, page, size));
    }

    @GetMapping("/dashboard")
    public DashboardResponse dashboard() {
        return inventoryQueryService.dashboard();
    }

    @PostMapping("/variants/{variantId}/adjust")
    public StockAdjustResponse adjust(
            @PathVariable Long variantId, @Valid @RequestBody StockAdjustRequest request, Authentication authentication) {
        return stockService.adjust(variantId, request, actingUserId(authentication));
    }

    @PostMapping("/variants/{variantId}/damage")
    public DamageResponse damage(
            @PathVariable Long variantId, @Valid @RequestBody DamageRequest request, Authentication authentication) {
        return stockService.recordDamage(variantId, request, actingUserId(authentication));
    }

    @GetMapping("/transactions")
    public PageResponse<InventoryTransactionRow> listTransactions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Long variantId,
            @RequestParam(required = false) InventoryTransactionType type,
            @RequestParam(required = false) Instant dateFrom,
            @RequestParam(required = false) Instant dateTo) {
        return PageResponse.of(inventoryQueryService.listTransactions(variantId, type, dateFrom, dateTo, page, size));
    }

    @GetMapping("/variants/{variantId}/transactions")
    public PageResponse<InventoryTransactionRow> listVariantTransactions(
            @PathVariable Long variantId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return PageResponse.of(inventoryQueryService.listVariantTransactions(variantId, page, size));
    }

    @GetMapping("/damages")
    public PageResponse<DamageRecordRow> listDamages(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Long variantId) {
        return PageResponse.of(inventoryQueryService.listDamages(variantId, page, size));
    }

    private Long actingUserId(Authentication authentication) {
        return authentication != null ? (Long) authentication.getPrincipal() : null;
    }
}
