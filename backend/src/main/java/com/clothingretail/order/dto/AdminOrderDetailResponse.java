package com.clothingretail.order.dto;

import com.clothingretail.order.OrderStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record AdminOrderDetailResponse(
        Long id,
        String orderNumber,
        OrderStatus status,
        List<OrderStatus> availableNextStatuses,
        List<AdminOrderItemResponse> items,
        BigDecimal subtotal,
        BigDecimal discountTotal,
        BigDecimal shippingCharge,
        BigDecimal totalAmount,
        AdminOrderCustomerResponse customer,
        OrderShippingAddressResponse shippingAddress,
        AdminPaymentResponse payment,
        AdminRefundResponse refund,
        AdminShipmentResponse shipment,
        List<AdminOrderStatusHistoryResponse> statusHistory,
        List<AdminOrderNoteResponse> notes,
        Instant createdAt,
        Instant updatedAt) {}
