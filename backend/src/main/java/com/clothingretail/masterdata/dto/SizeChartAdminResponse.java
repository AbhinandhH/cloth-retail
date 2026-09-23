package com.clothingretail.masterdata.dto;

import java.time.Instant;
import java.util.List;

public record SizeChartAdminResponse(
        Long id,
        String name,
        String description,
        int displayOrder,
        boolean active,
        List<String> columns,
        List<SizeChartRowResponse> rows,
        String createdByName,
        String updatedByName,
        Instant createdAt,
        Instant updatedAt) {}
