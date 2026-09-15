package com.clothingretail.tax.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record TaxSettingsUpdateRequest(
        @NotNull(message = "must not be null")
                @DecimalMin(value = "0", message = "must be between 0 and 100")
                @DecimalMax(value = "100", message = "must be between 0 and 100")
                BigDecimal cgstPercent,
        @NotNull(message = "must not be null")
                @DecimalMin(value = "0", message = "must be between 0 and 100")
                @DecimalMax(value = "100", message = "must be between 0 and 100")
                BigDecimal sgstPercent) {}
