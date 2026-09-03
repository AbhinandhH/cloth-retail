package com.clothingretail.inventory.dto;

import java.math.BigDecimal;

public record PurchaseItemResponse(
        Long id,
        Long productVariantId,
        String sku,
        int quantity,
        BigDecimal purchasePrice,
        BigDecimal sellingPrice) {}
