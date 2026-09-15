package com.clothingretail.returns.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record AdminInitiateRefundRequest(
        @NotNull(message = "must not be null") @DecimalMin(value = "0.01", message = "must be greater than zero") BigDecimal amount,
        String reason) {}
