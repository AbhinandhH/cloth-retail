package com.clothingretail.order.service;

import com.clothingretail.order.OrderStatus;
import java.util.Set;

/**
 * The single source of truth for the admin-driven order status graph - both the forward
 * lifecycle (CONFIRMED -&gt; PROCESSING -&gt; PACKED -&gt; SHIPPED -&gt; DELIVERED -&gt; RETURNED
 * -&gt; REFUNDED) and which statuses are eligible for cancellation. Nowhere else in the codebase
 * encodes this graph - {@code AdminOrderServiceImpl} calls only the methods here to validate and
 * apply a requested change, and {@code AdminSettableOrderStatus} makes the payment-only statuses
 * (and CONFIRMED/CANCELLED, neither of which is a valid {@code /status} target) structurally
 * unreachable before this class is ever consulted.
 *
 * Cancellation itself (including its stock-reversal branching) is orchestrated by
 * AdminOrderServiceImpl - this class only says whether it's allowed from a given status.
 */
public interface OrderTransitionService {

    /** What the GET /{id} response's {@code availableNextStatuses} field reports - forward moves plus CANCELLED, whichever apply from {@code current}. */
    Set<OrderStatus> availableNextStatuses(OrderStatus current);

    boolean isCancellable(OrderStatus current);

    /** Throws a ConflictException naming the current status, the rejected target, and the actually-valid forward moves from here - or does nothing if {@code requested} is reachable. */
    void validateForwardTransition(OrderStatus current, OrderStatus requested);

    /** Throws a ConflictException if {@code current} is not an eligible status to cancel from. */
    void validateCancellable(OrderStatus current);
}
