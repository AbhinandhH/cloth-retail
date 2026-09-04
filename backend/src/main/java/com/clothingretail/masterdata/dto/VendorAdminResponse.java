package com.clothingretail.masterdata.dto;

import java.time.Instant;

public record VendorAdminResponse(
        Long id,
        String name,
        String contactName,
        String contactEmail,
        String contactPhone,
        boolean active,
        String createdByName,
        String updatedByName,
        Instant createdAt,
        Instant updatedAt) {}
