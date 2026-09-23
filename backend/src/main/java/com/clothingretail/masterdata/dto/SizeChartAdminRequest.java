package com.clothingretail.masterdata.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record SizeChartAdminRequest(
        @NotBlank(message = "Enter a name for this size chart") String name,
        String description,
        Integer displayOrder,
        @NotNull(message = "Choose whether this chart is active") Boolean active,
        /** Ordered - position in this list is each column's position; every row's values must match this length and order. */
        @NotEmpty(message = "Add at least one measurement column")
                List<@NotBlank(message = "Column names can't be empty - remove any blank ones") String> columns,
        @Valid List<SizeChartRowRequest> rows) {}
