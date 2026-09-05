package com.clothingretail.order;

/**
 * The only target statuses an admin may request via {@code POST /api/admin/orders/{id}/status}.
 * Deliberately a separate, narrower enum from {@link OrderStatus} (rather than accepting
 * {@code OrderStatus} directly and rejecting bad values in code) so that a payment-only status
 * (PENDING_PAYMENT/PAYMENT_PROCESSING/PAYMENT_FAILED) - or CONFIRMED/CANCELLED, neither of which
 * is a valid `/status` target either (CONFIRMED is only reached via payment success; cancellation
 * has its own {@code /cancel} endpoint) - is structurally impossible to deserialize into this
 * request field in the first place: Jackson rejects an unknown enum constant with a 400 before
 * any application code runs.
 *
 * Constant names intentionally match {@link OrderStatus}'s so converting is a plain
 * {@code OrderStatus.valueOf(name())}.
 */
public enum AdminSettableOrderStatus {
    PROCESSING,
    PACKED,
    SHIPPED,
    DELIVERED,
    RETURNED,
    REFUNDED;

    public OrderStatus toOrderStatus() {
        return OrderStatus.valueOf(name());
    }
}
