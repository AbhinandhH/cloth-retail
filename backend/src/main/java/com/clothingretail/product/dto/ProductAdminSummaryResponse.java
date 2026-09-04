package com.clothingretail.product.dto;

import com.clothingretail.product.ProductStatus;
import java.time.Instant;

public record ProductAdminSummaryResponse(
        Long id,
        String name,
        String slug,
        String baseSku,
        String categoryName,
        String brandName,
        ProductStatus status,
        int variantCount,
        int totalStock,
        Instant updatedAt) {}
