package com.clothingretail.product.dto;

import com.clothingretail.product.ProductStatus;
import java.math.BigDecimal;
import java.util.List;

public record ProductAdminResponse(
        Long id,
        Long categoryId,
        String categoryName,
        Long subCategoryId,
        String subCategoryName,
        Long brandId,
        String brandName,
        Long materialId,
        String materialName,
        String name,
        String slug,
        String description,
        ProductStatus status,
        String baseSku,
        BigDecimal baseSellingPrice,
        BigDecimal baseCostPrice,
        List<VariantAdminResponse> variants) {}
