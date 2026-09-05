package com.clothingretail.order;

import com.clothingretail.auth.User;
import com.clothingretail.auth.UserRepository;
import com.clothingretail.common.AuditorNameResolver;
import com.clothingretail.common.ConflictException;
import com.clothingretail.common.NotFoundException;
import com.clothingretail.inventory.InventoryTransaction;
import com.clothingretail.inventory.InventoryTransactionRepository;
import com.clothingretail.inventory.InventoryTransactionType;
import com.clothingretail.order.dto.AdminOrderCancelRequest;
import com.clothingretail.order.dto.AdminOrderDetailResponse;
import com.clothingretail.order.dto.AdminOrderNoteRequest;
import com.clothingretail.order.dto.AdminOrderNoteResponse;
import com.clothingretail.order.dto.AdminOrderStatusUpdateRequest;
import com.clothingretail.order.dto.AdminRefundRequest;
import com.clothingretail.order.dto.AdminRefundResponse;
import com.clothingretail.order.dto.AdminShipmentRequest;
import com.clothingretail.order.dto.AdminShipmentResponse;
import com.clothingretail.payment.Payment;
import com.clothingretail.payment.PaymentGateway;
import com.clothingretail.payment.PaymentRepository;
import com.clothingretail.payment.PaymentStatus;
import com.clothingretail.payment.Refund;
import com.clothingretail.payment.RefundInitiation;
import com.clothingretail.payment.RefundRepository;
import com.clothingretail.payment.RefundStatus;
import com.clothingretail.product.ProductVariant;
import com.clothingretail.product.ProductVariantRepository;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Write-side of the admin order module: status transitions, cancellation (with its stock-restore
 * or reservation-release branching), notes, shipment upsert, and refund. Kept separate from
 * {@link AdminOrderQueryService} (the read side), same split as {@code StockService}/{@code
 * InventoryQueryService}. Every mutation here re-reads and returns the response shape via {@link
 * AdminOrderQueryService#toDetailResponse} (status/cancel) or its own narrow DTO (note/shipment/
 * refund), never duplicating that mapping logic.
 */
@Service
public class AdminOrderService {

    /** Stock was already decremented (via decrementStockOnSale) for an order in any of these statuses - cancelling one must restore it, not just release a reservation. */
    private static final Set<OrderStatus> STOCK_DECREMENTED_STATUSES =
            Set.of(OrderStatus.CONFIRMED, OrderStatus.PROCESSING, OrderStatus.PACKED);

    private final OrderRepository orderRepository;
    private final OrderTransitionService orderTransitionService;
    private final OrderStatusHistoryService orderStatusHistoryService;
    private final AdminOrderQueryService adminOrderQueryService;
    private final OrderNoteRepository orderNoteRepository;
    private final ShipmentRepository shipmentRepository;
    private final ProductVariantRepository productVariantRepository;
    private final InventoryTransactionRepository inventoryTransactionRepository;
    private final PaymentRepository paymentRepository;
    private final RefundRepository refundRepository;
    private final PaymentGateway paymentGateway;
    private final UserRepository userRepository;
    private final AuditorNameResolver auditorNameResolver;

    public AdminOrderService(
            OrderRepository orderRepository,
            OrderTransitionService orderTransitionService,
            OrderStatusHistoryService orderStatusHistoryService,
            AdminOrderQueryService adminOrderQueryService,
            OrderNoteRepository orderNoteRepository,
            ShipmentRepository shipmentRepository,
            ProductVariantRepository productVariantRepository,
            InventoryTransactionRepository inventoryTransactionRepository,
            PaymentRepository paymentRepository,
            RefundRepository refundRepository,
            PaymentGateway paymentGateway,
            UserRepository userRepository,
            AuditorNameResolver auditorNameResolver) {
        this.orderRepository = orderRepository;
        this.orderTransitionService = orderTransitionService;
        this.orderStatusHistoryService = orderStatusHistoryService;
        this.adminOrderQueryService = adminOrderQueryService;
        this.orderNoteRepository = orderNoteRepository;
        this.shipmentRepository = shipmentRepository;
        this.productVariantRepository = productVariantRepository;
        this.inventoryTransactionRepository = inventoryTransactionRepository;
        this.paymentRepository = paymentRepository;
        this.refundRepository = refundRepository;
        this.paymentGateway = paymentGateway;
        this.userRepository = userRepository;
        this.auditorNameResolver = auditorNameResolver;
    }

    @Transactional
    public AdminOrderDetailResponse updateStatus(Long orderId, AdminOrderStatusUpdateRequest request, Long adminUserId) {
        Order order = findOrder(orderId);
        OrderStatus current = order.getStatus();
        OrderStatus requested = request.toStatus().toOrderStatus();

        orderTransitionService.validateForwardTransition(current, requested);

        order.setStatus(requested);
        orderRepository.save(order);
        orderStatusHistoryService.record(order, current, requested, adminUserId, request.reason());

        return adminOrderQueryService.toDetailResponse(order);
    }

    @Transactional
    public AdminOrderDetailResponse cancel(Long orderId, AdminOrderCancelRequest request, Long adminUserId) {
        Order order = findOrder(orderId);
        OrderStatus current = order.getStatus();

        orderTransitionService.validateCancellable(current);

        boolean stockWasDecremented = STOCK_DECREMENTED_STATUSES.contains(current);
        User actingAdmin = adminUserId != null ? userRepository.findById(adminUserId).orElse(null) : null;

        for (OrderItem item : order.getItems()) {
            ProductVariant variant = item.getProductVariant();
            if (variant == null) {
                // Variant was deleted after the order was placed - nothing left to reverse/release against.
                continue;
            }
            if (stockWasDecremented) {
                restoreStockWithAudit(order, item, variant, actingAdmin);
            } else {
                // Never decremented (PENDING_PAYMENT/PAYMENT_PROCESSING) - only the reservation needs releasing, no InventoryTransaction, same reasoning as payment-failure release.
                productVariantRepository.releaseReservation(variant.getId(), item.getQuantity());
            }
        }

        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);
        orderStatusHistoryService.record(order, current, OrderStatus.CANCELLED, adminUserId, request.reason());

        return adminOrderQueryService.toDetailResponse(order);
    }

    private void restoreStockWithAudit(Order order, OrderItem item, ProductVariant variant, User actingAdmin) {
        int previousStock = productVariantRepository.getStockQuantity(variant.getId());
        productVariantRepository.restoreStockOnCancellation(variant.getId(), item.getQuantity());
        int newStock = productVariantRepository.getStockQuantity(variant.getId());

        InventoryTransaction transaction = new InventoryTransaction();
        transaction.setProductVariant(variant);
        transaction.setType(InventoryTransactionType.CANCEL_REVERSAL);
        transaction.setQuantity(item.getQuantity());
        transaction.setPreviousQuantity(previousStock);
        transaction.setNewQuantity(newStock);
        transaction.setReferenceType("ORDER");
        transaction.setReferenceId(order.getId());
        transaction.setReason("Cancellation - Order " + order.getOrderNumber());
        transaction.setPerformedBy(actingAdmin);
        inventoryTransactionRepository.save(transaction);
    }

    @Transactional
    public AdminOrderNoteResponse addNote(Long orderId, AdminOrderNoteRequest request, Long adminUserId) {
        Order order = findOrder(orderId);

        OrderNote note = new OrderNote();
        note.setOrder(order);
        note.setNote(request.note());
        note.setCreatedBy(adminUserId);
        OrderNote saved = orderNoteRepository.save(note);

        return new AdminOrderNoteResponse(saved.getId(), saved.getNote(), auditorNameResolver.resolve(adminUserId), saved.getCreatedAt());
    }

    @Transactional
    public AdminShipmentResponse upsertShipment(Long orderId, AdminShipmentRequest request) {
        Order order = findOrder(orderId);

        Shipment shipment = shipmentRepository.findByOrderId(orderId).orElseGet(() -> {
            Shipment created = new Shipment();
            created.setOrder(order);
            return created;
        });
        shipment.setProvider(request.provider());
        shipment.setTrackingNumber(request.trackingNumber());
        shipment.setShipmentDate(request.shipmentDate());
        shipment.setDeliveryDate(request.deliveryDate());
        shipment.setNotes(request.notes());
        Shipment saved = shipmentRepository.save(shipment);

        return new AdminShipmentResponse(
                saved.getProvider(), saved.getTrackingNumber(), saved.getShipmentDate(), saved.getDeliveryDate(), saved.getNotes());
    }

    @Transactional
    public AdminRefundResponse refund(Long orderId, AdminRefundRequest request, Long adminUserId) {
        findOrder(orderId);
        Payment payment = paymentRepository.findFirstByOrderIdOrderByIdDesc(orderId)
                .orElseThrow(() -> new ConflictException("Order has no payment to refund"));
        if (payment.getStatus() != PaymentStatus.SUCCESS) {
            throw new ConflictException("Cannot refund a payment that has not succeeded (status: " + payment.getStatus() + ")");
        }

        java.math.BigDecimal alreadyRefunded = refundRepository.sumCompletedAmountByPaymentId(payment.getId());
        java.math.BigDecimal remaining = payment.getAmount().subtract(alreadyRefunded);
        if (request.amount().compareTo(remaining) > 0) {
            throw new ConflictException(
                    "Refund amount " + request.amount() + " exceeds the remaining refundable amount " + remaining
                            + " (already refunded: " + alreadyRefunded + " of " + payment.getAmount() + ")");
        }

        // Purely a payment-side reversal - does NOT restore inventory (cancellation's job, a separate action).
        RefundInitiation initiation = paymentGateway.refund(payment, request.amount());

        Refund refund = new Refund();
        refund.setPayment(payment);
        refund.setAmount(request.amount());
        refund.setStatus(RefundStatus.COMPLETED);
        refund.setReference(initiation.reference());
        refund.setInitiatedBy(adminUserId);
        Refund saved = refundRepository.save(refund);

        return new AdminRefundResponse(saved.getId(), saved.getAmount(), saved.getStatus(), saved.getReference(), saved.getCreatedAt());
    }

    private Order findOrder(Long orderId) {
        return orderRepository.findById(orderId).orElseThrow(() -> new NotFoundException("Order not found: " + orderId));
    }
}
