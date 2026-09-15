package com.clothingretail.order.service;

import com.clothingretail.order.Order;
import com.clothingretail.order.OrderStatus;
import com.clothingretail.order.dto.AdminOrderDashboardResponse;
import com.clothingretail.order.dto.AdminOrderDetailResponse;
import com.clothingretail.order.dto.AdminOrderRow;
import com.clothingretail.payment.PaymentStatus;
import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Page;

/**
 * Read-side of the admin order module: the paginated/filterable list, the dashboard aggregate,
 * and the single-order detail view - kept separate from AdminOrderService (the write
 * side), same split as InventoryQueryService/StockService.
 */
public interface AdminOrderQueryService {

    Page<AdminOrderRow> list(
            String q,
            OrderStatus orderStatus,
            PaymentStatus paymentStatus,
            Instant dateFrom,
            Instant dateTo,
            String paymentMethod,
            String sort,
            String dir,
            int page,
            int size);

    AdminOrderDashboardResponse dashboard();

    AdminOrderDetailResponse detail(Long orderId);

    /** Builds the full detail shape for a single, already-loaded order - a handful of O(1) queries scoped to this one order, never a per-row loop. */
    AdminOrderDetailResponse toDetailResponse(Order order);

    /** Maps a page of orders to list/dashboard rows in O(1) extra queries total (batched item-count + latest-payment lookups), never per-row. */
    List<AdminOrderRow> toRows(List<Order> orders);
}
