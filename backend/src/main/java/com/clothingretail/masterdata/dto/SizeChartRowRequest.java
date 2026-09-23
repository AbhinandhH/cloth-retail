package com.clothingretail.masterdata.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

public record SizeChartRowRequest(
        @NotBlank(message = "Enter a size label for this row, e.g. \"M\" or \"42\"") String sizeLabel,
        /** Positionally aligned to the parent chart request's columns list - must be the same length. */
        List<String> values) {}
