package com.clothingretail.inventory.dto;

import com.clothingretail.inventory.InventoryTransactionType;
import java.time.Instant;

public record InventoryTransactionRow(
        Long id,
        Long variantId,
        String sku,
        String productName,
        InventoryTransactionType type,
        int quantity,
        int previousQuantity,
        int newQuantity,
        String reason,
        String referenceType,
        Long referenceId,
        String performedByName,
        Instant createdAt) {}
