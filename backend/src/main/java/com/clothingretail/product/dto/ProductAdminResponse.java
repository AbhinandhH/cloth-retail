package com.clothingretail.product.dto;

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
        boolean active,
        List<VariantAdminResponse> variants) {}
