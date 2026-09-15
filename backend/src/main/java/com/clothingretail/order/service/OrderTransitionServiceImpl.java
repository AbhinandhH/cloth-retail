package com.clothingretail.order.service;

import com.clothingretail.common.ConflictException;
import com.clothingretail.order.OrderStatus;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

@Service
@Log4j2
public class OrderTransitionServiceImpl implements OrderTransitionService {

    /** Forward, admin-initiated moves reachable via {@code POST /{id}/status}. Terminal/system statuses simply have no entry (empty set). */
    private static final Map<OrderStatus, Set<OrderStatus>> FORWARD_TRANSITIONS = Map.of(
            OrderStatus.CONFIRMED, Set.of(OrderStatus.PROCESSING),
            OrderStatus.PROCESSING, Set.of(OrderStatus.PACKED),
            OrderStatus.PACKED, Set.of(OrderStatus.SHIPPED),
            OrderStatus.SHIPPED, Set.of(OrderStatus.DELIVERED),
            OrderStatus.DELIVERED, Set.of(OrderStatus.RETURNED),
            OrderStatus.RETURNED, Set.of(OrderStatus.REFUNDED));

    /**
     * A SHIPPED/DELIVERED (or later) order needs the return flow instead - see
     * AdminOrderService.cancel. PENDING_PAYMENT/PAYMENT_PROCESSING are included because stock was
     * only ever reserved for them, never decremented - cancelling just releases the reservation.
     */
    private static final Set<OrderStatus> CANCELLABLE_FROM = Set.of(
            OrderStatus.PENDING_PAYMENT,
            OrderStatus.PAYMENT_PROCESSING,
            OrderStatus.CONFIRMED,
            OrderStatus.PROCESSING,
            OrderStatus.PACKED);

    /** What the GET /{id} response's {@code availableNextStatuses} field reports - forward moves plus CANCELLED, whichever apply from {@code current}. */
    @Override
    public Set<OrderStatus> availableNextStatuses(OrderStatus current) {
        log.info("[1623] Computing available next statuses for current={}", current);
        Set<OrderStatus> next = new LinkedHashSet<>(FORWARD_TRANSITIONS.getOrDefault(current, Set.of()));
        if (CANCELLABLE_FROM.contains(current)) {
            next.add(OrderStatus.CANCELLED);
        }
        return next;
    }

    @Override
    public boolean isCancellable(OrderStatus current) {
        boolean cancellable = CANCELLABLE_FROM.contains(current);
        log.info("[1624] isCancellable check: current={}, cancellable={}", current, cancellable);
        return cancellable;
    }

    /** Throws a {@link ConflictException} naming the current status, the rejected target, and the actually-valid forward moves from here - or does nothing if {@code requested} is reachable. */
    @Override
    public void validateForwardTransition(OrderStatus current, OrderStatus requested) {
        Set<OrderStatus> allowed = FORWARD_TRANSITIONS.getOrDefault(current, Set.of());
        if (!allowed.contains(requested)) {
            log.error("[1625] Invalid forward transition requested: {} -> {} (allowed from {}: {})", current, requested, current, allowed);
            throw new ConflictException(
                    "Cannot move order from " + current + " to " + requested + " - valid next status(es) from "
                            + current + ": " + (allowed.isEmpty() ? "none" : allowed));
        }
        log.info("[1626] Forward transition validated: {} -> {}", current, requested);
    }

    /** Throws a {@link ConflictException} if {@code current} is not an eligible status to cancel from. */
    @Override
    public void validateCancellable(OrderStatus current) {
        if (!isCancellable(current)) {
            String suffix = (current == OrderStatus.SHIPPED || current == OrderStatus.DELIVERED)
                    ? " - use the return flow instead"
                    : "";
            log.error("[1627] Cancellation rejected: order status {} is not cancellable", current);
            throw new ConflictException("Order in status " + current + " cannot be cancelled" + suffix);
        }
        log.info("[1628] Cancellation validated: order status {} is cancellable", current);
    }
}
