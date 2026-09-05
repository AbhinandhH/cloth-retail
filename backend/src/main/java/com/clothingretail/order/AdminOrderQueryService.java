package com.clothingretail.order;

import com.clothingretail.auth.User;
import com.clothingretail.common.AuditorNameResolver;
import com.clothingretail.common.NotFoundException;
import com.clothingretail.order.dto.AdminOrderCustomerResponse;
import com.clothingretail.order.dto.AdminOrderDashboardResponse;
import com.clothingretail.order.dto.AdminOrderDetailResponse;
import com.clothingretail.order.dto.AdminOrderItemResponse;
import com.clothingretail.order.dto.AdminOrderNoteResponse;
import com.clothingretail.order.dto.AdminOrderRow;
import com.clothingretail.order.dto.AdminOrderStatusHistoryResponse;
import com.clothingretail.order.dto.AdminPaymentResponse;
import com.clothingretail.order.dto.AdminRefundResponse;
import com.clothingretail.order.dto.AdminShipmentResponse;
import com.clothingretail.order.dto.OrderShippingAddressResponse;
import com.clothingretail.payment.Payment;
import com.clothingretail.payment.PaymentRepository;
import com.clothingretail.payment.PaymentStatus;
import com.clothingretail.payment.RefundRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Read-side of the admin order module: the paginated/filterable list, the dashboard aggregate,
 * and the single-order detail view - kept separate from {@link AdminOrderService} (the write
 * side), same split as {@code InventoryQueryService}/{@code StockService}. {@link
 * #toDetailResponse} is also called by {@code AdminOrderService} to build the "refreshed full
 * detail" return value of its status/cancel mutations, so the response shape is built in exactly
 * one place.
 */
@Service
@Transactional(readOnly = true)
public class AdminOrderQueryService {

    private static final int DASHBOARD_RECENT_LIMIT = 10;

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final RefundRepository refundRepository;
    private final ShipmentRepository shipmentRepository;
    private final OrderStatusHistoryRepository orderStatusHistoryRepository;
    private final OrderNoteRepository orderNoteRepository;
    private final OrderTransitionService orderTransitionService;
    private final AuditorNameResolver auditorNameResolver;

    public AdminOrderQueryService(
            OrderRepository orderRepository,
            PaymentRepository paymentRepository,
            RefundRepository refundRepository,
            ShipmentRepository shipmentRepository,
            OrderStatusHistoryRepository orderStatusHistoryRepository,
            OrderNoteRepository orderNoteRepository,
            OrderTransitionService orderTransitionService,
            AuditorNameResolver auditorNameResolver) {
        this.orderRepository = orderRepository;
        this.paymentRepository = paymentRepository;
        this.refundRepository = refundRepository;
        this.shipmentRepository = shipmentRepository;
        this.orderStatusHistoryRepository = orderStatusHistoryRepository;
        this.orderNoteRepository = orderNoteRepository;
        this.orderTransitionService = orderTransitionService;
        this.auditorNameResolver = auditorNameResolver;
    }

    public Page<AdminOrderRow> list(
            String q,
            OrderStatus orderStatus,
            PaymentStatus paymentStatus,
            Instant dateFrom,
            Instant dateTo,
            String paymentMethod,
            String sort,
            String dir,
            int page,
            int size) {
        Pageable pageable = PageRequest.of(page, size, sort(sort, dir));
        var spec = OrderSpecifications.filter(q, orderStatus, paymentStatus, dateFrom, dateTo, paymentMethod);
        Page<Order> orders = orderRepository.findAll(spec, pageable);
        return new PageImpl<>(toRows(orders.getContent()), pageable, orders.getTotalElements());
    }

    public AdminOrderDashboardResponse dashboard() {
        long totalOrders = orderRepository.count();
        long pendingCount = orderRepository.countByStatus(OrderStatus.CONFIRMED);
        long processingCount = orderRepository.countByStatus(OrderStatus.PROCESSING);
        long packedCount = orderRepository.countByStatus(OrderStatus.PACKED);
        long shippedCount = orderRepository.countByStatus(OrderStatus.SHIPPED);
        long deliveredCount = orderRepository.countByStatus(OrderStatus.DELIVERED);
        long cancelledCount = orderRepository.countByStatus(OrderStatus.CANCELLED);
        long paymentFailedCount = orderRepository.countByStatus(OrderStatus.PAYMENT_FAILED);

        var spec = OrderSpecifications.filter(null, null, null, null, null, null);
        Pageable recentPageable = PageRequest.of(0, DASHBOARD_RECENT_LIMIT, Sort.by(Sort.Direction.DESC, "createdAt"));
        List<Order> recent = orderRepository.findAll(spec, recentPageable).getContent();

        return new AdminOrderDashboardResponse(
                totalOrders,
                pendingCount,
                processingCount,
                packedCount,
                shippedCount,
                deliveredCount,
                cancelledCount,
                paymentFailedCount,
                toRows(recent));
    }

    public AdminOrderDetailResponse detail(Long orderId) {
        Order order = orderRepository.findById(orderId).orElseThrow(() -> new NotFoundException("Order not found: " + orderId));
        return toDetailResponse(order);
    }

    /** Builds the full detail shape for a single, already-loaded order - a handful of O(1) queries scoped to this one order, never a per-row loop. */
    AdminOrderDetailResponse toDetailResponse(Order order) {
        List<AdminOrderItemResponse> items = order.getItems().stream().map(this::toItemResponse).toList();

        List<OrderStatusHistory> historyRows = orderStatusHistoryRepository.findByOrderIdOrderByIdAsc(order.getId());
        List<OrderNote> noteRows = orderNoteRepository.findByOrderIdOrderByIdAsc(order.getId());

        Set<Long> actorIds = new HashSet<>();
        historyRows.forEach(h -> {
            if (h.getChangedBy() != null) {
                actorIds.add(h.getChangedBy());
            }
        });
        noteRows.forEach(n -> actorIds.add(n.getCreatedBy()));
        Map<Long, String> actorNames = auditorNameResolver.resolveNames(actorIds);

        List<AdminOrderStatusHistoryResponse> statusHistory = historyRows.stream()
                .map(h -> new AdminOrderStatusHistoryResponse(
                        h.getPreviousStatus(),
                        h.getNewStatus(),
                        h.getChangedBy() != null ? actorNames.get(h.getChangedBy()) : null,
                        h.getReason(),
                        h.getCreatedAt()))
                .toList();
        List<AdminOrderNoteResponse> notes = noteRows.stream()
                .map(n -> new AdminOrderNoteResponse(n.getId(), n.getNote(), actorNames.get(n.getCreatedBy()), n.getCreatedAt()))
                .toList();

        Payment payment = paymentRepository.findFirstByOrderIdOrderByIdDesc(order.getId()).orElse(null);
        AdminPaymentResponse paymentResponse = payment == null
                ? null
                : new AdminPaymentResponse(
                        payment.getId(),
                        payment.getStatus(),
                        payment.getPaymentMethod(),
                        payment.getAmount(),
                        payment.getGatewayReference(),
                        null,
                        payment.getCreatedAt());

        AdminRefundResponse refundResponse = payment == null
                ? null
                : refundRepository.findFirstByPaymentIdOrderByIdDesc(payment.getId())
                        .map(r -> new AdminRefundResponse(r.getId(), r.getAmount(), r.getStatus(), r.getReference(), r.getCreatedAt()))
                        .orElse(null);

        AdminShipmentResponse shipmentResponse = shipmentRepository.findByOrderId(order.getId())
                .map(s -> new AdminShipmentResponse(
                        s.getProvider(), s.getTrackingNumber(), s.getShipmentDate(), s.getDeliveryDate(), s.getNotes()))
                .orElse(null);

        User user = order.getCustomerProfile().getUser();
        AdminOrderCustomerResponse customer = new AdminOrderCustomerResponse(user.getFullName(), user.getMobileNumber(), user.getEmail());

        OrderShippingAddressResponse shippingAddress = new OrderShippingAddressResponse(
                order.getShippingAddressLine1(),
                order.getShippingAddressLine2(),
                order.getShippingCity(),
                order.getShippingState(),
                order.getShippingPostalCode(),
                order.getShippingCountry());

        List<OrderStatus> availableNextStatuses = new ArrayList<>(orderTransitionService.availableNextStatuses(order.getStatus()));

        return new AdminOrderDetailResponse(
                order.getId(),
                order.getOrderNumber(),
                order.getStatus(),
                availableNextStatuses,
                items,
                order.getSubtotal(),
                order.getDiscountTotal(),
                order.getShippingCharge(),
                order.getTotalAmount(),
                customer,
                shippingAddress,
                paymentResponse,
                refundResponse,
                shipmentResponse,
                statusHistory,
                notes,
                order.getCreatedAt(),
                order.getUpdatedAt());
    }

    /** Maps a page of orders to list/dashboard rows in O(1) extra queries total (batched item-count + latest-payment lookups), never per-row. */
    private List<AdminOrderRow> toRows(List<Order> orders) {
        if (orders.isEmpty()) {
            return List.of();
        }
        List<Long> orderIds = orders.stream().map(Order::getId).toList();

        Map<Long, Long> itemCounts = new HashMap<>();
        for (OrderRepository.OrderItemCountProjection projection : orderRepository.sumItemCountsByOrderIds(orderIds)) {
            itemCounts.put(projection.getOrderId(), projection.getItemCount());
        }

        Map<Long, Payment> latestPaymentByOrderId = new HashMap<>();
        for (Payment payment : paymentRepository.findLatestByOrderIds(orderIds)) {
            latestPaymentByOrderId.put(payment.getOrder().getId(), payment);
        }

        return orders.stream()
                .map(order -> {
                    User user = order.getCustomerProfile().getUser();
                    Payment payment = latestPaymentByOrderId.get(order.getId());
                    long itemCount = itemCounts.getOrDefault(order.getId(), 0L);
                    return new AdminOrderRow(
                            order.getId(),
                            order.getOrderNumber(),
                            user.getFullName(),
                            contact(user),
                            order.getStatus(),
                            payment != null ? payment.getStatus() : null,
                            (int) itemCount,
                            order.getTotalAmount(),
                            order.getCreatedAt(),
                            order.getUpdatedAt());
                })
                .toList();
    }

    private String contact(User user) {
        return (user.getMobileNumber() != null && !user.getMobileNumber().isBlank()) ? user.getMobileNumber() : user.getEmail();
    }

    private AdminOrderItemResponse toItemResponse(OrderItem item) {
        return new AdminOrderItemResponse(
                item.getId(),
                item.getProductName(),
                item.getSku(),
                item.getColorName(),
                item.getSizeName(),
                item.getImageUrl(),
                item.getQuantity(),
                item.getUnitPrice(),
                item.getDiscountPercent(),
                item.getLineTotal());
    }

    private Sort sort(String sort, String dir) {
        Sort.Direction direction = "asc".equalsIgnoreCase(dir) ? Sort.Direction.ASC : Sort.Direction.DESC;
        String property =
                switch (sort == null ? "" : sort) {
                    case "totalAmount" -> "totalAmount";
                    case "status" -> "status";
                    default -> "createdAt";
                };
        return Sort.by(direction, property);
    }
}
