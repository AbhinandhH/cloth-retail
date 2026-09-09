package com.clothingretail.product.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record VariantAdminRequest(
        // Present on update to identify an existing variant; null/absent means "create a new variant".
        Long id,
        @NotBlank(message = "must not be blank") String sku,
        @NotNull(message = "must not be null") Long sizeId,
        @NotNull(message = "must not be null") Long colorId,
        @NotNull(message = "must not be null") @DecimalMin(value = "0", inclusive = true, message = "must be >= 0") BigDecimal sellingPrice,
        @DecimalMin(value = "0", inclusive = true, message = "must be >= 0") BigDecimal discountPercent,
        @DecimalMin(value = "0", inclusive = true, message = "must be >= 0") BigDecimal costPrice,
        // Only used when creating a brand-new variant (id == null) to set its initial stock.
        // Ignored entirely when updating an existing variant - stock changes for existing
        // variants can only happen through the adjust/damage endpoints.
        @Min(value = 0, message = "must be >= 0") Integer stockQuantity,
        @Min(value = 0, message = "must be >= 0") Integer lowStockThreshold,
        Boolean active) {}
