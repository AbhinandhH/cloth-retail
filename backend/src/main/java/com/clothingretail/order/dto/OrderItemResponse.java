package com.clothingretail.order.dto;

import java.math.BigDecimal;

public record OrderItemResponse(
        Long id,
        String productName,
        String sku,
        String colorName,
        String sizeName,
        int quantity,
        BigDecimal unitPrice,
        BigDecimal discountPercent,
        BigDecimal lineTotal) {}
