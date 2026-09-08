package com.clothingretail.dashboard.dto;

import com.clothingretail.order.dto.AdminOrderRow;
import java.math.BigDecimal;
import java.util.List;

/**
 * The admin landing dashboard's full payload. Deliberately assembled from the existing
 * order/inventory dashboards' own aggregates (see AdminDashboardQueryService) rather than
 * re-deriving those counts here, so the numbers on this page can never drift from the
 * per-module Orders/Inventory dashboards that already show them.
 */
public record AdminDashboardResponse(
        BigDecimal todaysSales,
        long todaysOrderCount,
        long totalOrders,
        long pendingOrders,
        long productsInStock,
        long lowStockCount,
        long outOfStockCount,
        List<AdminOrderRow> recentOrders,
        List<SalesOverviewPoint> salesOverview,
        List<TopSellingProductRow> topSellingProducts) {}
