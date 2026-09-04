package com.clothingretail.inventory.dto;

import java.util.List;

public record DashboardResponse(
        long totalProducts,
        long totalVariants,
        long totalAvailableStock,
        long lowStockCount,
        long outOfStockCount,
        long totalDamagedStock,
        List<VariantInventoryRow> lowStockItems,
        List<VariantInventoryRow> outOfStockItems,
        List<RecentProductRow> recentProducts,
        List<InventoryTransactionRow> recentTransactions,
        List<DamageRecordRow> recentDamages) {}
