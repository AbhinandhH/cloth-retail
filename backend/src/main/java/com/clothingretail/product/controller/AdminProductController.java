package com.clothingretail.product.controller;

import com.clothingretail.common.PageResponse;
import com.clothingretail.product.ProductStatus;
import com.clothingretail.product.dto.ProductAdminRequest;
import com.clothingretail.product.dto.ProductAdminResponse;
import com.clothingretail.product.dto.ProductAdminSummaryResponse;
import com.clothingretail.product.dto.ProductStatusUpdateRequest;
import com.clothingretail.product.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/products")
@PreAuthorize("hasRole('ADMIN') or (hasRole('EMPLOYEE') and @modulePermission.hasAccess('PRODUCTS'))")
public class AdminProductController {

    private final ProductService productService;

    public AdminProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    public PageResponse<ProductAdminSummaryResponse> list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) ProductStatus status,
            @PageableDefault(size = 20) Pageable pageable) {
        return PageResponse.of(productService.listAdmin(q, categoryId, status, pageable));
    }

    @GetMapping("/{id}")
    public ProductAdminResponse get(@PathVariable Long id) {
        return productService.getAdmin(id);
    }

    @PostMapping
    public ResponseEntity<ProductAdminResponse> create(
            @Valid @RequestBody ProductAdminRequest request, Authentication authentication) {
        return ResponseEntity.ok(productService.create(request, actingUserId(authentication)));
    }

    @PutMapping("/{id}")
    public ProductAdminResponse update(
            @PathVariable Long id, @Valid @RequestBody ProductAdminRequest request, Authentication authentication) {
        return productService.update(id, request, actingUserId(authentication));
    }

    @PatchMapping("/{id}/status")
    public ProductAdminResponse updateStatus(@PathVariable Long id, @Valid @RequestBody ProductStatusUpdateRequest request) {
        return productService.updateStatus(id, request.status());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        productService.delete(id);
        return ResponseEntity.noContent().build();
    }

    private Long actingUserId(Authentication authentication) {
        return authentication != null ? (Long) authentication.getPrincipal() : null;
    }
}
