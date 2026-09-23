package com.clothingretail.product.dto;

import java.util.List;

public record ProductDetailResponse(
        Long id,
        String slug,
        String name,
        String description,
        String categoryName,
        String subCategoryName,
        String brand,
        String material,
        ProductSizeChartResponse sizeChart,
        List<VariantResponse> variants) {}
