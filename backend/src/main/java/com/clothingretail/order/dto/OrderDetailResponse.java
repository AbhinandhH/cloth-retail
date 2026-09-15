package com.clothingretail.order.dto;

import com.clothingretail.order.OrderStatus;
import com.clothingretail.payment.PaymentStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OrderDetailResponse(
        Long id,
        String orderNumber,
        OrderStatus status,
        PaymentStatus paymentStatus,
        List<OrderItemResponse> items,
        BigDecimal subtotal,
        BigDecimal discountTotal,
        BigDecimal shippingCharge,
        BigDecimal cgstPercent,
        BigDecimal cgstAmount,
        BigDecimal sgstPercent,
        BigDecimal sgstAmount,
        BigDecimal totalAmount,
        OrderShippingAddressResponse shippingAddress,
        String contactName,
        String contactPhone,
        Instant reservationExpiresAt,
        Instant createdAt) {}
