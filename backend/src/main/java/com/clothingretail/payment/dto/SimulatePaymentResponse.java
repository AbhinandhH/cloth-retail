package com.clothingretail.payment.dto;

import com.clothingretail.order.OrderStatus;
import com.clothingretail.payment.PaymentStatus;

public record SimulatePaymentResponse(Long orderId, OrderStatus orderStatus, PaymentStatus paymentStatus) {}
