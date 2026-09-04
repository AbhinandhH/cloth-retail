package com.clothingretail.inventory.dto;

import com.clothingretail.inventory.StockStatus;
import com.clothingretail.product.ProductStatus;
import java.math.BigDecimal;
import java.time.Instant;

public record VariantInventoryRow(
        Long variantId,
        Long productId,
        String productName,
        String productSlug,
        String sku,
        String categoryName,
        String colorName,
        String colorHex,
        String sizeName,
        BigDecimal sellingPrice,
        BigDecimal costPrice,
        int stockQuantity,
        int reservedQuantity,
        int damagedQuantity,
        int availableQuantity,
        Integer lowStockThreshold,
        StockStatus stockStatus,
        boolean active,
        ProductStatus productStatus,
        Instant updatedAt) {}
