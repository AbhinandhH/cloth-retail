package com.clothingretail.returns;

/**
 * Real workflow state that drives code branching - a plain enum, not a master-data table (same
 * reasoning as OrderStatus/PaymentStatus/RefundStatus). Deliberately just 4 values - other
 * lifecycle information the feature needs (video-evidence progress, "admin verification status")
 * is already fully captured by {@link EvidenceStatus} and this enum together, so a separate
 * "under review"/"awaiting video" status here would only ever duplicate one of those.
 */
public enum ReturnRequestStatus {
    /** Created by the customer, awaiting an admin decision. Both request types start here. */
    PENDING,
    /**
     * SIZE_EXCHANGE: terminal - the inventory swap has already executed atomically as part of
     * reaching this status (see ReturnAdminServiceImpl#approveExchange).
     * DAMAGED_PRODUCT: non-terminal - the damage claim is accepted and a refund can now be
     * initiated, but hasn't been yet.
     */
    APPROVED,
    /** Terminal for both request types. */
    REJECTED,
    /** DAMAGED_PRODUCT-only terminal state, set once the delegated refund call succeeds. */
    REFUNDED
}
