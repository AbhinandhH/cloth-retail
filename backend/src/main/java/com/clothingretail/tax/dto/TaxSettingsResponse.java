package com.clothingretail.tax.dto;

import java.math.BigDecimal;

public record TaxSettingsResponse(BigDecimal cgstPercent, BigDecimal sgstPercent) {}
