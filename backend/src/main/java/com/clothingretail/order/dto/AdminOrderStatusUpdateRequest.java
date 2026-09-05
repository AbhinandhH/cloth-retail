package com.clothingretail.order.dto;

import com.clothingretail.order.AdminSettableOrderStatus;
import jakarta.validation.constraints.NotNull;

/**
 * {@code toStatus} deserializes only into {@link AdminSettableOrderStatus} - a payment-only
 * status (or CONFIRMED/CANCELLED) is structurally impossible to submit here; Jackson rejects an
 * unrecognized enum constant with a 400 before {@code AdminOrderService} ever runs.
 */
public record AdminOrderStatusUpdateRequest(
        @NotNull(message = "must not be null") AdminSettableOrderStatus toStatus, String reason) {}
