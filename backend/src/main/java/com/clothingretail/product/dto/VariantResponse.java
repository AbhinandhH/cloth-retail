package com.clothingretail.product.dto;

import java.math.BigDecimal;
import java.util.List;

public record VariantResponse(
        Long id,
        String sku,
        String sizeName,
        String colorName,
        String colorHex,
        BigDecimal sellingPrice,
        BigDecimal discountPercent,
        int stockQuantity,
        List<String> images) {}
