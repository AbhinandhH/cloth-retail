package com.clothingretail.returns.dto;

import jakarta.validation.constraints.NotNull;

public record CreateExchangeRequest(@NotNull(message = "must not be null") Long requestedVariantId, String reason) {}
