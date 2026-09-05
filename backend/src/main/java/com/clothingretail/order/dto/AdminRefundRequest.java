package com.clothingretail.order.dto;

import java.math.BigDecimal;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

public record AdminRefundRequest(
        @NotNull(message = "must not be null") @DecimalMin(value = "0.01", message = "must be greater than zero") BigDecimal amount,
        String reason) {}
