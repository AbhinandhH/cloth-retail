package com.clothingretail.inventory.dto;

import java.time.Instant;

public record DamageRecordRow(
        Long id,
        Long variantId,
        String sku,
        String productName,
        int quantity,
        Long reasonId,
        String reasonName,
        String notes,
        String reportedByName,
        Instant createdAt) {}
