package com.clothingretail.product.dto;

import java.math.BigDecimal;

public record ProductSummaryResponse(
        Long id,
        String slug,
        String name,
        String brand,
        String primaryImageUrl,
        BigDecimal minPrice,
        BigDecimal maxPrice,
        BigDecimal discountPercent,
        String categoryName,
        boolean inStock) {}
