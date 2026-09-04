package com.clothingretail.inventory.dto;

import com.clothingretail.inventory.DamageReason;
import java.time.Instant;

public record DamageRecordRow(
        Long id,
        Long variantId,
        String sku,
        String productName,
        int quantity,
        DamageReason reason,
        String notes,
        String reportedByName,
        Instant createdAt) {}
