package com.clothingretail.order.dto;

import java.math.BigDecimal;

public record AdminOrderItemResponse(
        Long id,
        String productName,
        String sku,
        String colorName,
        String sizeName,
        String imageUrl,
        int quantity,
        BigDecimal unitPrice,
        BigDecimal discountPercent,
        BigDecimal lineTotal) {}
