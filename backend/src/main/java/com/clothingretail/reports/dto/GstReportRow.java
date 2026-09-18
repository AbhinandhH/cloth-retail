package com.clothingretail.reports.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record GstReportRow(
        String orderNumber, Instant createdAt, BigDecimal cgstAmount, BigDecimal sgstAmount, BigDecimal totalAmount) {}
