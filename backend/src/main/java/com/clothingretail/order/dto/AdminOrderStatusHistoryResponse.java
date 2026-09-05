package com.clothingretail.order.dto;

import com.clothingretail.order.OrderStatus;
import java.time.Instant;

/** {@code changedByName} is null for a system-driven transition - the admin UI displays that as "System". */
public record AdminOrderStatusHistoryResponse(
        OrderStatus previousStatus, OrderStatus newStatus, String changedByName, String reason, Instant createdAt) {}
