package com.clothingretail.masterdata.dto;

import java.time.Instant;

public record SizeAdminResponse(
        Long id,
        String name,
        int displayOrder,
        boolean active,
        String createdByName,
        String updatedByName,
        Instant createdAt,
        Instant updatedAt) {}
