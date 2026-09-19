package com.clothingretail.auth.dto;

import jakarta.validation.constraints.NotNull;

public record UpdateStaffStatusRequest(@NotNull(message = "must not be null") Boolean enabled) {}
