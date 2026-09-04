package com.clothingretail.product.dto;

import com.clothingretail.product.ProductStatus;
import jakarta.validation.constraints.NotNull;

public record ProductStatusUpdateRequest(@NotNull(message = "must not be null") ProductStatus status) {}
