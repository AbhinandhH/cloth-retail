package com.clothingretail.masterdata.dto;

import java.time.Instant;

public record CategoryAdminResponse(
        Long id,
        String name,
        String slug,
        int displayOrder,
        boolean active,
        String createdByName,
        String updatedByName,
        Instant createdAt,
        Instant updatedAt) {}
