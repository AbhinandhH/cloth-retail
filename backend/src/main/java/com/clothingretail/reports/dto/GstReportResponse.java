package com.clothingretail.reports.dto;

import com.clothingretail.common.PageResponse;
import java.math.BigDecimal;

/** {@code page} covers only the current page's rows; the three totals are grand totals across the whole matching date range, not just this page. */
public record GstReportResponse(
        PageResponse<GstReportRow> page, BigDecimal totalCgst, BigDecimal totalSgst, BigDecimal totalRevenue) {}
