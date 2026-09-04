package com.clothingretail.masterdata.dto;

import java.time.Instant;

public record ColorAdminResponse(
        Long id,
        String name,
        String hexCode,
        int displayOrder,
        boolean active,
        String createdByName,
        String updatedByName,
        Instant createdAt,
        Instant updatedAt) {}
