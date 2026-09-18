package com.clothingretail.reports.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record SalesSummaryRow(LocalDate date, long orderCount, BigDecimal revenue) {}
