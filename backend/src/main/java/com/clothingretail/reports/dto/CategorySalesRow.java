package com.clothingretail.reports.dto;

import java.math.BigDecimal;

public record CategorySalesRow(String categoryName, long quantitySold, BigDecimal revenue) {}
