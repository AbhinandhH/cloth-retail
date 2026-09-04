package com.clothingretail.product.dto;

import java.math.BigDecimal;
import java.util.List;

public record VariantAdminResponse(
        Long id,
        String sku,
        Long sizeId,
        String sizeName,
        Long colorId,
        String colorName,
        BigDecimal sellingPrice,
        BigDecimal discountPercent,
        BigDecimal costPrice,
        int stockQuantity,
        int reservedQuantity,
        int damagedQuantity,
        int availableQuantity,
        Integer lowStockThreshold,
        boolean active,
        List<String> images) {}
