package com.clothingretail.product.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record ProductAdminRequest(
        @NotNull(message = "must not be null") Long categoryId,
        Long subCategoryId,
        @NotNull(message = "must not be null") Long brandId,
        @NotNull(message = "must not be null") Long materialId,
        @NotBlank(message = "must not be blank") String name,
        @NotBlank(message = "must not be blank") String slug,
        String description,
        Boolean active,
        @Valid List<VariantAdminRequest> variants) {}
