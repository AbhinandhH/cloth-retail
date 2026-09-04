package com.clothingretail.masterdata.dto;

import java.time.Instant;
import java.util.List;

public record SizeGroupAdminResponse(
        Long id,
        String name,
        String description,
        int displayOrder,
        boolean active,
        List<Long> categoryIds,
        List<String> categoryNames,
        List<SizeGroupSizeResponse> sizes,
        String createdByName,
        String updatedByName,
        Instant createdAt,
        Instant updatedAt) {}
