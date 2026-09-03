package com.clothingretail.inventory.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record PurchaseItemRequest(
        @NotNull(message = "must not be null") Long productVariantId,
        @NotNull(message = "must not be null") @Min(value = 1, message = "must be at least 1") Integer quantity,
        @NotNull(message = "must not be null") @DecimalMin(value = "0", message = "must be >= 0") BigDecimal purchasePrice,
        @DecimalMin(value = "0", message = "must be >= 0") BigDecimal sellingPrice) {}
