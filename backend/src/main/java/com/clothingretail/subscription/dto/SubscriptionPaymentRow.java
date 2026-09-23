package com.clothingretail.subscription.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record SubscriptionPaymentRow(Long id, BigDecimal amount, String status, Instant createdAt) {}
