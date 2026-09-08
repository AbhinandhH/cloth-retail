package com.clothingretail.customer.dto;

import jakarta.validation.constraints.NotNull;

public record AdminCustomerStatusUpdateRequest(@NotNull(message = "must not be null") Boolean enabled) {}
