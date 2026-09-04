package com.clothingretail.cart.dto;

import java.math.BigDecimal;

public record CartItemResponse(
        Long id,
        Long productVariantId,
        String productName,
        String productSlug,
        String sku,
        String colorName,
        String colorHex,
        String sizeName,
        String imageUrl,
        BigDecimal unitPrice,
        BigDecimal discountPercent,
        BigDecimal lineTotal,
        int quantity,
        int availableQuantity,
        boolean active) {}
