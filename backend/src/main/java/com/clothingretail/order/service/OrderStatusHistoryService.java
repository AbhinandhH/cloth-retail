package com.clothingretail.order.service;

import com.clothingretail.order.Order;
import com.clothingretail.order.OrderStatus;

/**
 * The single helper every status change - system-driven or admin-driven - goes through to append
 * an {@code OrderStatusHistory} row. Deliberately does no validation of its own: callers (
 * OrderCreationService, PaymentServiceImpl, PaymentWebhookServiceImpl,
 * OrderReservationCleanupJob for system-driven changes; OrderTransitionService for
 * admin-driven ones) have each already decided the new status is correct by the time they call
 * this - it only ever records what already happened.
 */
public interface OrderStatusHistoryService {

    /**
     * @param changedBy acting admin's user id, or {@code null} for a system-driven transition
     *     (order creation, payment initiation/webhook outcome, reservation expiry) - the admin UI
     *     displays a {@code null} changedByName as "System".
     */
    void record(Order order, OrderStatus previousStatus, OrderStatus newStatus, Long changedBy, String reason);
}
