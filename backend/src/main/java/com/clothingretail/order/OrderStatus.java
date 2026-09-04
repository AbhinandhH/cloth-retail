package com.clothingretail.order;

/** Real workflow state that drives code branching - a plain enum, not a master-data table (same reasoning as ProductStatus). */
public enum OrderStatus {
    PENDING_PAYMENT,
    PAYMENT_PROCESSING,
    PAYMENT_FAILED,
    CONFIRMED,
    CANCELLED,
    COMPLETED
}
