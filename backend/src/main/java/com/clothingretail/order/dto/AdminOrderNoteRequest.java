package com.clothingretail.order.dto;

import jakarta.validation.constraints.NotBlank;

public record AdminOrderNoteRequest(@NotBlank(message = "must not be blank") String note) {}
