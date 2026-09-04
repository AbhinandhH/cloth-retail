package com.clothingretail.payment;

/** Real workflow state that drives code branching - a plain enum, not a master-data table (same reasoning as ProductStatus/OrderStatus). */
public enum PaymentStatus {
    PENDING,
    SUCCESS,
    FAILED
}
