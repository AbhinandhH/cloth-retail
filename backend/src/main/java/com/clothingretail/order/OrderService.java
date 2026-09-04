package com.clothingretail.order;

import com.clothingretail.common.ConflictException;
import com.clothingretail.common.NotFoundException;
import com.clothingretail.common.PageResponse;
import com.clothingretail.customer.CustomerProfile;
import com.clothingretail.customer.CustomerProfileRepository;
import com.clothingretail.order.dto.CreateOrderRequest;
import com.clothingretail.order.dto.OrderDetailResponse;
import com.clothingretail.order.dto.OrderItemResponse;
import com.clothingretail.order.dto.OrderShippingAddressResponse;
import com.clothingretail.order.dto.OrderSummaryResponse;
import com.clothingretail.payment.Payment;
import com.clothingretail.payment.PaymentRepository;
import com.clothingretail.payment.PaymentStatus;
import java.util.Optional;
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
        Optional<Order> existing = orderRepository.findByIdempotencyKey(request.idempotencyKey());
        if (existing.isPresent()) {
            return toDetailResponse(requireOwned(existing.get(), userId));
        }

        try {
            Order created = orderCreationService.create(userId, request);
            return toDetailResponse(created);
        } catch (DataIntegrityViolationException raceLoss) {
            // Someone else's identical request (same idempotency key) won the insert race in the
            // moment between our lookup above and our own insert attempt. Our transaction already
            // rolled back (nothing we reserved is applied) - fetch and return the winner's order
            // instead of surfacing an error for what is, from the client's point of view, a
            // successful duplicate submission.
            Order winner = orderRepository.findByIdempotencyKey(request.idempotencyKey()).orElseThrow(() -> raceLoss);
            return toDetailResponse(requireOwned(winner, userId));
        }
    }

    @Transactional(readOnly = true)
    public PageResponse<OrderSummaryResponse> listOrders(Long userId, int page, int size) {
        CustomerProfile profile = resolveProfile(userId);
        Page<Order> orders = orderRepository.findByCustomerProfileId(
                profile.getId(), PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        return PageResponse.of(orders, orders.getContent().stream().map(this::toSummaryResponse).toList());
    }

    @Transactional(readOnly = true)
    public OrderDetailResponse getOrder(Long userId, Long orderId) {
        CustomerProfile profile = resolveProfile(userId);
        Order order = orderRepository.findByIdAndCustomerProfileId(orderId, profile.getId())
                .orElseThrow(() -> new NotFoundException("Order not found: " + orderId));
        return toDetailResponse(order);
    }

    private Order requireOwned(Order order, Long userId) {
        CustomerProfile profile = resolveProfile(userId);
        if (!order.getCustomerProfile().getId().equals(profile.getId())) {
            // Idempotency keys are client-generated and expected to be unique per client -
            // a collision across two different customers means one of them reused someone
            // else's key, which we refuse rather than leak/return the other customer's order.
            throw new ConflictException("This idempotency key was already used for a different order");
        }
        return order;
    }

    private CustomerProfile resolveProfile(Long userId) {
        return customerProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new NotFoundException("Customer profile not found for user: " + userId));
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
                order.getTotalAmount(),
                shippingAddress,
                order.getContactName(),
                order.getContactPhone(),
                order.getReservationExpiresAt(),
                order.getCreatedAt());
    }
}
