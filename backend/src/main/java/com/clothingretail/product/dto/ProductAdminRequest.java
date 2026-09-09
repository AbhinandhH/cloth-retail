package com.clothingretail.product.dto;

import com.clothingretail.product.ProductStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;

public record ProductAdminRequest(
        @NotNull(message = "must not be null") Long categoryId,
        Long subCategoryId,
        // Brand is optional (see Product.brand).
        Long brandId,
        @NotNull(message = "must not be null") Long materialId,
        // Nullable at the DB/entity level (see Product.vendor's own doc comment) but required
        // here - every new product must specify a vendor.
        @NotNull(message = "must not be null") Long vendorId,
        @NotBlank(message = "must not be blank") String name,
        @NotBlank(message = "must not be blank") String slug,
        String description,
        ProductStatus status,
        String baseSku,
        @DecimalMin(value = "0", inclusive = true, message = "must be >= 0") BigDecimal baseSellingPrice,
        @DecimalMin(value = "0", inclusive = true, message = "must be >= 0") BigDecimal baseCostPrice,
        @Valid List<VariantAdminRequest> variants,
        @Valid List<ColorImagesRequest> colorImages) {}
