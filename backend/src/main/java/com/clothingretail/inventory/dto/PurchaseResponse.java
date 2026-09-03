package com.clothingretail.inventory.dto;

import java.time.Instant;
import java.util.List;

public record PurchaseResponse(
        Long id,
        Long vendorId,
        String vendorName,
        String invoiceNumber,
        Instant purchaseDate,
        List<PurchaseItemResponse> items) {}
