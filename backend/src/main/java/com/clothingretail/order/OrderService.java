package com.clothingretail.order;

import com.clothingretail.common.ConflictException;
import com.clothingretail.common.NotFoundException;
import com.clothingretail.common.PageResponse;
import com.clothingretail.customer.CustomerProfile;
import com.clothingretail.customer.repository.CustomerProfileRepository;
import com.clothingretail.order.dto.CreateOrderRequest;
import com.clothingretail.order.dto.OrderDetailResponse;
import com.clothingretail.order.dto.OrderItemResponse;
import com.clothingretail.order.dto.OrderShippingAddressResponse;
import com.clothingretail.order.dto.OrderSummaryResponse;
import com.clothingretail.payment.Payment;
import com.clothingretail.payment.PaymentRepository;
import com.clothingretail.payment.PaymentStatus;
import java.util.Optional;
import lombok.extern.log4j.Log4j2;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Public-facing order API. {@link #createOrder} is deliberately NOT {@code @Transactional} itself
 * - it orchestrates a fast idempotency-key lookup, then delegates the actual (transactional)
 * creation work to {@link OrderCreationService}, a separate bean, so that a
 * {@link DataIntegrityViolationException} thrown by a losing concurrent request can be caught
 * here, after that transaction has already rolled back, and turned into "return the winner's
 * order" instead of an error.
 */
@Service
@Log4j2
public class OrderService {

    private final OrderRepository orderRepository;
    private final CustomerProfileRepository customerProfileRepository;
    private final PaymentRepository paymentRepository;
    private final OrderCreationService orderCreationService;

    public OrderService(
            OrderRepository orderRepository,
            CustomerProfileRepository customerProfileRepository,
            PaymentRepository paymentRepository,
            OrderCreationService orderCreationService) {
        this.orderRepository = orderRepository;
        this.customerProfileRepository = customerProfileRepository;
        this.paymentRepository = paymentRepository;
        this.orderCreationService = orderCreationService;
    }

    public OrderDetailResponse createOrder(Long userId, CreateOrderRequest request) {
        log.info("[1612] createOrder requested: userId={}, idempotencyKey={}", userId, request.idempotencyKey());
        Optional<Order> existing = orderRepository.findByIdempotencyKey(request.idempotencyKey());
        if (existing.isPresent()) {
            log.info("[1613] Idempotency fast-path: existing order {} found for idempotencyKey={}",
                    existing.get().getId(), request.idempotencyKey());
            return toDetailResponse(requireOwned(existing.get(), userId));
        }

        try {
            Order created = orderCreationService.create(userId, request);
            log.info("[1614] Order create-path succeeded: orderId={}, idempotencyKey={}", created.getId(), request.idempotencyKey());
            return toDetailResponse(created);
        } catch (DataIntegrityViolationException raceLoss) {
            // Someone else's identical request (same idempotency key) won the insert race in the
            // moment between our lookup above and our own insert attempt. Our transaction already
            // rolled back (nothing we reserved is applied) - fetch and return the winner's order
            // instead of surfacing an error for what is, from the client's point of view, a
            // successful duplicate submission.
            log.error("[1615] Insert race lost for idempotencyKey={}, userId={} - looking up winner",
                    request.idempotencyKey(), userId, raceLoss);
            Order winner = orderRepository.findByIdempotencyKey(request.idempotencyKey()).orElseThrow(() -> raceLoss);
            log.info("[1616] Race winner order {} returned for idempotencyKey={}", winner.getId(), request.idempotencyKey());
            return toDetailResponse(requireOwned(winner, userId));
        }
    }

    @Transactional(readOnly = true)
    public PageResponse<OrderSummaryResponse> listOrders(Long userId, int page, int size) {
        log.info("[1617] Listing orders for userId={}, page={}, size={}", userId, page, size);
        CustomerProfile profile = resolveProfile(userId);
        Page<Order> orders = orderRepository.findByCustomerProfileId(
                profile.getId(), PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        log.info("[1618] Listed {} orders (of {} total) for userId={}", orders.getNumberOfElements(), orders.getTotalElements(), userId);
        return PageResponse.of(orders, orders.getContent().stream().map(this::toSummaryResponse).toList());
    }

    @Transactional(readOnly = true)
    public OrderDetailResponse getOrder(Long userId, Long orderId) {
        log.info("[1619] Fetching order {} for userId={}", orderId, userId);
        CustomerProfile profile = resolveProfile(userId);
        Order order = orderRepository.findByIdAndCustomerProfileId(orderId, profile.getId())
                .orElseThrow(() -> {
                    log.error("[1620] Order {} not found for userId={}", orderId, userId);
                    return new NotFoundException("Order not found: " + orderId);
                });
        return toDetailResponse(order);
    }

    private Order requireOwned(Order order, Long userId) {
        CustomerProfile profile = resolveProfile(userId);
        if (!order.getCustomerProfile().getId().equals(profile.getId())) {
            // Idempotency keys are client-generated and expected to be unique per client -
            // a collision across two different customers means one of them reused someone
            // else's key, which we refuse rather than leak/return the other customer's order.
            log.error("[1621] Idempotency key collision: order {} does not belong to userId={}", order.getId(), userId);
            throw new ConflictException("This idempotency key was already used for a different order");
        }
        return order;
    }

    private CustomerProfile resolveProfile(Long userId) {
        return customerProfileRepository.findByUserId(userId)
                .orElseThrow(() -> {
                    log.error("[1622] Customer profile not found for userId={}", userId);
                    return new NotFoundException("Customer profile not found for user: " + userId);
                });
    }

    private OrderSummaryResponse toSummaryResponse(Order order) {
        int itemCount = order.getItems().stream().mapToInt(OrderItem::getQuantity).sum();
        return new OrderSummaryResponse(
                order.getId(), order.getOrderNumber(), order.getStatus(), order.getTotalAmount(), itemCount, order.getCreatedAt());
    }

    private OrderDetailResponse toDetailResponse(Order order) {
        var items = order.getItems().stream()
                .map(i -> new OrderItemResponse(
                        i.getId(), i.getProductName(), i.getSku(), i.getColorName(), i.getSizeName(),
                        i.getQuantity(), i.getUnitPrice(), i.getDiscountPercent(), i.getLineTotal()))
                .toList();

        var shippingAddress = new OrderShippingAddressResponse(
                order.getShippingAddressLine1(), order.getShippingAddressLine2(), order.getShippingCity(),
                order.getShippingState(), order.getShippingPostalCode(), order.getShippingCountry());

        PaymentStatus paymentStatus = paymentRepository.findFirstByOrderIdOrderByIdDesc(order.getId())
                .map(Payment::getStatus)
                .orElse(null);

        return new OrderDetailResponse(
                order.getId(),
                order.getOrderNumber(),
                order.getStatus(),
                paymentStatus,
                items,
                order.getSubtotal(),
                order.getDiscountTotal(),
                order.getShippingCharge(),
                order.getCgstPercent(),
                order.getCgstAmount(),
                order.getSgstPercent(),
                order.getSgstAmount(),
                order.getTotalAmount(),
                shippingAddress,
                order.getContactName(),
                order.getContactPhone(),
                order.getReservationExpiresAt(),
                order.getCreatedAt());
    }
}
