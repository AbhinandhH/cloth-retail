package com.clothingretail.order.dto;

import java.util.List;

public record AdminOrderDashboardResponse(
        long totalOrders,
        long pendingCount,
        long processingCount,
        long packedCount,
        long shippedCount,
        long deliveredCount,
        long cancelledCount,
        long paymentFailedCount,
        List<AdminOrderRow> recentOrders) {}
