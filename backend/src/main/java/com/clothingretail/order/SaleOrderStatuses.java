package com.clothingretail.order;

import java.util.List;

/**
 * Orders in these statuses represent an actual completed sale - excludes ones that never
 * finished payment (PENDING_PAYMENT/PAYMENT_PROCESSING/PAYMENT_FAILED) and ones called off
 * afterward (CANCELLED). RETURNED/REFUNDED are deliberately still counted as a real sale for the
 * day/customer it happened against - a later return doesn't rewrite that history; the
 * order/inventory dashboards already surface returns separately if that detail is needed.
 *
 * Shared by the admin landing dashboard's "today's sales" figure (AdminDashboardQueryService)
 * and the admin Customers module's "total orders"/"total amount spent" per customer
 * (AdminCustomerQueryService) - both concepts mean the same thing (a real, paid-for sale) and
 * must stay in agreement, or a customer's profile could show a different order count/spend than
 * what actually rolled up into the day they placed it.
 */
public final class SaleOrderStatuses {

    private SaleOrderStatuses() {}

    public static final List<OrderStatus> SALE_STATUSES = List.of(
            OrderStatus.CONFIRMED,
            OrderStatus.PROCESSING,
            OrderStatus.PACKED,
            OrderStatus.SHIPPED,
            OrderStatus.DELIVERED,
            OrderStatus.RETURNED,
            OrderStatus.REFUNDED);
}
