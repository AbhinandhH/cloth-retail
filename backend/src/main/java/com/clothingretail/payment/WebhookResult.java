package com.clothingretail.payment;

import com.clothingretail.order.OrderStatus;

record WebhookResult(Long orderId, OrderStatus orderStatus, PaymentStatus paymentStatus) {}
