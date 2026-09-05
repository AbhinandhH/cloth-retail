package com.clothingretail.order;

/**
 * Real workflow state that drives code branching - a plain enum, not a master-data table (same
 * reasoning as ProductStatus). PENDING_PAYMENT/PAYMENT_PROCESSING/PAYMENT_FAILED are
 * system-driven (order creation / payment initiation / webhook outcome). CONFIRMED onward through
 * DELIVERED/RETURNED/REFUNDED (plus CANCELLED) are the admin-driven post-payment lifecycle - see
 * {@link OrderTransitionService} for the one place that graph is encoded.
 */
public enum OrderStatus {
    PENDING_PAYMENT,
    PAYMENT_PROCESSING,
    PAYMENT_FAILED,
    CONFIRMED,
    PROCESSING,
    PACKED,
    SHIPPED,
    DELIVERED,
    CANCELLED,
    RETURNED,
    REFUNDED
}
