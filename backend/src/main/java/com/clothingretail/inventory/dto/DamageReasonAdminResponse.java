package com.clothingretail.inventory.dto;

import java.time.Instant;

public record DamageReasonAdminResponse(
        Long id,
        String name,
        String code,
        String description,
        int displayOrder,
        boolean active,
        String createdByName,
        String updatedByName,
        Instant createdAt,
        Instant updatedAt) {}
