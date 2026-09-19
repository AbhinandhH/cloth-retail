package com.clothingretail.inventory.controller;

import com.clothingretail.inventory.dto.PurchaseRequest;
import com.clothingretail.inventory.dto.PurchaseResponse;
import com.clothingretail.inventory.service.PurchaseService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/purchases")
@PreAuthorize("hasRole('ADMIN') or (hasRole('EMPLOYEE') and @modulePermission.hasAccess('INVENTORY'))")
public class AdminPurchaseController {

    private final PurchaseService purchaseService;

    public AdminPurchaseController(PurchaseService purchaseService) {
        this.purchaseService = purchaseService;
    }

    @PostMapping
    public ResponseEntity<PurchaseResponse> create(@Valid @RequestBody PurchaseRequest request) {
        return ResponseEntity.ok(purchaseService.createPurchase(request));
    }

    @GetMapping
    public List<PurchaseResponse> list() {
        return purchaseService.listPurchases();
    }

    @GetMapping("/{id}")
    public PurchaseResponse get(@PathVariable Long id) {
        return purchaseService.getPurchase(id);
    }
}
