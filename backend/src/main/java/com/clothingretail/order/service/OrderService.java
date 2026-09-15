package com.clothingretail.order.service;

import com.clothingretail.common.PageResponse;
import com.clothingretail.order.dto.CreateOrderRequest;
import com.clothingretail.order.dto.OrderDetailResponse;
import com.clothingretail.order.dto.OrderSummaryResponse;

/**
 * Public-facing order API. {@link #createOrder} is deliberately NOT transactional itself
 * - it orchestrates a fast idempotency-key lookup, then delegates the actual (transactional)
 * creation work to OrderCreationService, a separate bean, so that a
 * DataIntegrityViolationException thrown by a losing concurrent request can be caught
 * here, after that transaction has already rolled back, and turned into "return the winner's
 * order" instead of an error.
 */
public interface OrderService {

    OrderDetailResponse createOrder(Long userId, CreateOrderRequest request);

    PageResponse<OrderSummaryResponse> listOrders(Long userId, int page, int size);

    OrderDetailResponse getOrder(Long userId, Long orderId);
}
