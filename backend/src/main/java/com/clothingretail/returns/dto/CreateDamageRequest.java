package com.clothingretail.returns.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateDamageRequest(@NotBlank(message = "must not be blank") String reason) {}
