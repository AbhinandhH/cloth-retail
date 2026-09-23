package com.clothingretail.subscription.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record SubscriptionStatusResponse(
        BigDecimal monthlyAmount, LocalDate dueDate, boolean paid, boolean locked, Long daysUntilDue) {}
