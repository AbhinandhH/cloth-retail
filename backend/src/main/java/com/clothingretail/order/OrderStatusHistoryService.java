package com.clothingretail.order;

import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The single helper every status change - system-driven or admin-driven - goes through to append
 * an {@link OrderStatusHistory} row. Deliberately does no validation of its own: callers ({@code
 * OrderCreationService}, {@code PaymentService}, {@code PaymentWebhookService}, {@code
 * OrderReservationCleanupJob} for system-driven changes; {@link OrderTransitionService} for
 * admin-driven ones) have each already decided the new status is correct by the time they call
 * this - it only ever records what already happened. Public (not package-private) because two of
 * its callers live in the {@code payment} package.
 *
 * {@code @Transactional} here simply joins whatever transaction is already open at each call
 * site (all of them are themselves {@code @Transactional}) - it never starts a second one.
 */
@Service
@Log4j2
public class OrderStatusHistoryService {

    private final OrderStatusHistoryRepository orderStatusHistoryRepository;

    public OrderStatusHistoryService(OrderStatusHistoryRepository orderStatusHistoryRepository) {
        this.orderStatusHistoryRepository = orderStatusHistoryRepository;
    }

    /**
     * @param changedBy acting admin's user id, or {@code null} for a system-driven transition
     *     (order creation, payment initiation/webhook outcome, reservation expiry) - the admin UI
     *     displays a {@code null} changedByName as "System".
     */
    @Transactional
    public void record(Order order, OrderStatus previousStatus, OrderStatus newStatus, Long changedBy, String reason) {
        log.info("[1656] Recording status history for order {}: {} -> {}, changedBy={}, reason={}",
                order.getId(), previousStatus, newStatus, changedBy, reason);
        OrderStatusHistory history = new OrderStatusHistory();
        history.setOrder(order);
        history.setPreviousStatus(previousStatus);
        history.setNewStatus(newStatus);
        history.setChangedBy(changedBy);
        history.setReason(reason);
        orderStatusHistoryRepository.save(history);
    }
}
