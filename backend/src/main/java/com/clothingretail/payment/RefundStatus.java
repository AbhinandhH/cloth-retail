package com.clothingretail.payment;

/** The mock gateway always resolves a refund immediately, so PENDING is never actually persisted today - kept for shape-completeness against a real gateway. */
public enum RefundStatus {
    PENDING,
    COMPLETED,
    FAILED
}
