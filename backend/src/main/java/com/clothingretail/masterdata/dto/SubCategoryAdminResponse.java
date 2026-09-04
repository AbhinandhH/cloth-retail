package com.clothingretail.masterdata.dto;

import java.time.Instant;

public record SubCategoryAdminResponse(
        Long id,
        String name,
        String slug,
        Long categoryId,
        String categoryName,
        int displayOrder,
        boolean active,
        String createdByName,
        String updatedByName,
        Instant createdAt,
        Instant updatedAt) {}
