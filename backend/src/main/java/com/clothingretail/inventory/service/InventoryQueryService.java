package com.clothingretail.inventory.service;

import com.clothingretail.inventory.InventoryTransactionType;
import com.clothingretail.inventory.StockStatus;
import com.clothingretail.inventory.dto.DamageRecordRow;
import com.clothingretail.inventory.dto.DashboardResponse;
import com.clothingretail.inventory.dto.InventoryTransactionRow;
import com.clothingretail.inventory.dto.VariantInventoryRow;
import com.clothingretail.product.ProductStatus;
import java.time.Instant;
import org.springframework.data.domain.Page;

/**
 * Read-side of the admin inventory module: the variant-level listing with filters/sort,
 * transaction and damage history listings, and the dashboard aggregate. Kept separate from
 * StockService (the write side) to keep each class focused.
 */
public interface InventoryQueryService {

    Page<VariantInventoryRow> listVariants(
            String q,
            Long categoryId,
            Long productId,
            Long colorId,
            Long sizeId,
            StockStatus stockStatus,
            ProductStatus productStatus,
            String sort,
            String dir,
            int page,
            int size);

    Page<InventoryTransactionRow> listTransactions(
            Long variantId, InventoryTransactionType type, Instant dateFrom, Instant dateTo, int page, int size);

    Page<InventoryTransactionRow> listVariantTransactions(Long variantId, int page, int size);

    Page<DamageRecordRow> listDamages(Long variantId, int page, int size);

    DashboardResponse dashboard();
}
