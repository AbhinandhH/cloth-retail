package com.clothingretail.masterdata.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record SizeGroupAdminRequest(
        @NotBlank(message = "must not be blank") String name,
        String description,
        Integer displayOrder,
        @NotNull(message = "must not be null") Boolean active,
        List<Long> categoryIds,
        /** Ordered - position in this list becomes each size's displayOrder within the group. */
        List<Long> sizeIds) {}
