package com.clothingretail.order.dto;

import com.clothingretail.order.OrderStatus;
import com.clothingretail.payment.PaymentStatus;
import java.math.BigDecimal;
import java.time.Instant;

/** One row of the admin order list/dashboard's recentOrders - see AdminOrderQueryService for how this avoids N+1. */
public record AdminOrderRow(
        Long id,
        String orderNumber,
        String customerName,
        String customerContact,
        OrderStatus status,
        PaymentStatus paymentStatus,
        int itemCount,
        BigDecimal totalAmount,
        Instant createdAt,
        Instant updatedAt) {}
