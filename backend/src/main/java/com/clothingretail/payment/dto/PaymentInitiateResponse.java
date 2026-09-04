package com.clothingretail.payment.dto;

import java.math.BigDecimal;

public record PaymentInitiateResponse(Long paymentId, Long orderId, BigDecimal amount, String gatewayReference) {}
