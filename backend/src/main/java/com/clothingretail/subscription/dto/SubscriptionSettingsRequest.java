package com.clothingretail.subscription.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

public record SubscriptionSettingsRequest(
        @NotNull(message = "Enter a monthly amount") @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
                BigDecimal monthlyAmount,
        @NotNull(message = "Choose a due date") @Future(message = "Due date must be in the future") LocalDate dueDate) {}
