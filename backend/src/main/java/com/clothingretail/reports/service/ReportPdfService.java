package com.clothingretail.reports.service;

import com.clothingretail.inventory.StockStatus;
import java.time.Instant;

/**
 * PDF-rendering side of the Reports module - one method per report, mirroring {@link
 * ReportsQueryService}'s shape but always pulling the full (capped) matching result set rather
 * than one page, since an export means "everything matching the filter," not "the page currently
 * on screen." See ReportPdfServiceImpl for the shared Thymeleaf+jsoup+openhtmltopdf render path.
 */
public interface ReportPdfService {

    byte[] salesSummaryPdf(Instant dateFrom, Instant dateTo);

    byte[] productSalesPdf(Instant dateFrom, Instant dateTo, Long productId);

    byte[] categorySalesPdf(Instant dateFrom, Instant dateTo, Long categoryId);

    byte[] customerSalesPdf(Instant dateFrom, Instant dateTo, Long customerId);

    byte[] stockPdf(Long categoryId, Long productId, StockStatus stockStatus);

    byte[] gstPdf(Instant dateFrom, Instant dateTo);
}
