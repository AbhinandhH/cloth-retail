package com.clothingretail.masterdata.dto;

import java.time.Instant;

public record MaterialAdminResponse(
        Long id,
        String name,
        int displayOrder,
        boolean active,
        String createdByName,
        String updatedByName,
        Instant createdAt,
        Instant updatedAt) {}
