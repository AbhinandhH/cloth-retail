package com.clothingretail.order.dto;

import jakarta.validation.constraints.NotBlank;

public record AdminOrderCancelRequest(@NotBlank(message = "must not be blank") String reason) {}
