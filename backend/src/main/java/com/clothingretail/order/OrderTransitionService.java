package com.clothingretail.order;

import com.clothingretail.common.ConflictException;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;

/**
 * The single source of truth for the admin-driven order status graph - both the forward
 * lifecycle (CONFIRMED -&gt; PROCESSING -&gt; PACKED -&gt; SHIPPED -&gt; DELIVERED -&gt; RETURNED
 * -&gt; REFUNDED) and which statuses are eligible for cancellation. Nowhere else in the codebase
 * encodes this graph - {@code AdminOrderService} calls only the methods here to validate and
 * apply a requested change, and {@link AdminSettableOrderStatus} makes the payment-only statuses
 * (and CONFIRMED/CANCELLED, neither of which is a valid {@code /status} target) structurally
 * unreachable before this class is ever consulted.
 *
 * Cancellation itself (including its stock-reversal branching) is orchestrated by {@code
 * AdminOrderService} - this class only says whether it's allowed from a given status.
 */
@Service
public class OrderTransitionService {

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
    public Set<OrderStatus> availableNextStatuses(OrderStatus current) {
        Set<OrderStatus> next = new LinkedHashSet<>(FORWARD_TRANSITIONS.getOrDefault(current, Set.of()));
        if (CANCELLABLE_FROM.contains(current)) {
            next.add(OrderStatus.CANCELLED);
        }
        return next;
    }

    public boolean isCancellable(OrderStatus current) {
        return CANCELLABLE_FROM.contains(current);
    }

    /** Throws a {@link ConflictException} naming the current status, the rejected target, and the actually-valid forward moves from here - or does nothing if {@code requested} is reachable. */
    public void validateForwardTransition(OrderStatus current, OrderStatus requested) {
        Set<OrderStatus> allowed = FORWARD_TRANSITIONS.getOrDefault(current, Set.of());
        if (!allowed.contains(requested)) {
            throw new ConflictException(
                    "Cannot move order from " + current + " to " + requested + " - valid next status(es) from "
                            + current + ": " + (allowed.isEmpty() ? "none" : allowed));
        }
    }

    /** Throws a {@link ConflictException} if {@code current} is not an eligible status to cancel from. */
    public void validateCancellable(OrderStatus current) {
        if (!isCancellable(current)) {
            String suffix = (current == OrderStatus.SHIPPED || current == OrderStatus.DELIVERED)
                    ? " - use the return flow instead"
                    : "";
            throw new ConflictException("Order in status " + current + " cannot be cancelled" + suffix);
        }
    }
}
