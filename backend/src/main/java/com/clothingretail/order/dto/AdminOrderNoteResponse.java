package com.clothingretail.order.dto;

import java.time.Instant;

public record AdminOrderNoteResponse(Long id, String note, String adminName, Instant createdAt) {}
