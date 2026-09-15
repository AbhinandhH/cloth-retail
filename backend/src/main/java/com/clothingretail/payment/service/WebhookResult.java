package com.clothingretail.payment.service;

import com.clothingretail.order.OrderStatus;
import com.clothingretail.payment.PaymentStatus;

record WebhookResult(Long orderId, OrderStatus orderStatus, PaymentStatus paymentStatus) {}
