package com.clothingretail.reports.dto;

import java.math.BigDecimal;

public record CustomerSalesRow(
        Long customerProfileId, String fullName, String email, long orderCount, BigDecimal totalSpent) {}
