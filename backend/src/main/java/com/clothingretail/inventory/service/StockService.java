package com.clothingretail.inventory.service;

import com.clothingretail.inventory.dto.DamageRequest;
import com.clothingretail.inventory.dto.DamageResponse;
import com.clothingretail.inventory.dto.StockAdjustRequest;
import com.clothingretail.inventory.dto.StockAdjustResponse;
import com.clothingretail.product.ProductVariant;

/**
 * The only place {@code ProductVariant.stockQuantity}/{@code damagedQuantity} change for an
 * already-existing variant (besides PurchaseService, which is the one place stock
 * increases via a purchase). Every mutation here also appends an immutable
 * InventoryTransaction row in the same transaction, so the audit log and the live
 * counters can never drift apart.
 */
public interface StockService {

    StockAdjustResponse adjust(Long variantId, StockAdjustRequest request, Long actingUserId);

    DamageResponse recordDamage(Long variantId, DamageRequest request, Long actingUserId);

    /**
     * Writes the one-off ADJUSTMENT transaction for a brand-new variant's initial stock
     * (set via the product form on create - see {@code ProductServiceImpl.applyVariants}).
     * Does NOT touch stockQuantity itself - the caller already set it before saving the
     * variant; this only records the audit trail entry.
     */
    void recordInitialStock(ProductVariant variant, int initialQuantity, Long actingUserId);
}
