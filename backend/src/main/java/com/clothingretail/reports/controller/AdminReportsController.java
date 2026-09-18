package com.clothingretail.reports.controller;

import com.clothingretail.common.PageResponse;
import com.clothingretail.inventory.StockStatus;
import com.clothingretail.inventory.dto.VariantInventoryRow;
import com.clothingretail.reports.dto.CategorySalesRow;
import com.clothingretail.reports.dto.CustomerSalesRow;
import com.clothingretail.reports.dto.GstReportResponse;
import com.clothingretail.reports.dto.ProductSalesRow;
import com.clothingretail.reports.dto.SalesSummaryResponse;
import com.clothingretail.reports.service.ReportPdfService;
import com.clothingretail.reports.service.ReportsQueryService;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Admin Reports module: sales summary, product/category/customer-wise sales, stock, and GST -
 * each with an on-screen (paginated JSON) endpoint and a matching {@code /export} PDF endpoint
 * sharing the same filters. Same ADMIN-or-SUPER_ADMIN tier as Order/Inventory management - reports
 * surface data those roles already see elsewhere, not the stricter SUPER_ADMIN-only tier reserved
 * for store-wide settings (Tax/Configuration).
 */
@RestController
@RequestMapping("/api/admin/reports")
@PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
public class AdminReportsController {

    // Sales-type reports default to the trailing 30 days when no range is given - a sane
    // "recent activity" default for a report screen landing with no filters applied yet.
    private static final int DEFAULT_RANGE_DAYS = 30;
    private static final DateTimeFormatter FILENAME_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final ReportsQueryService reportsQueryService;
    private final ReportPdfService reportPdfService;

    public AdminReportsController(ReportsQueryService reportsQueryService, ReportPdfService reportPdfService) {
        this.reportsQueryService = reportsQueryService;
        this.reportPdfService = reportPdfService;
    }

    @GetMapping("/sales-summary")
    public SalesSummaryResponse salesSummary(
            @RequestParam(required = false) Instant dateFrom, @RequestParam(required = false) Instant dateTo) {
        return reportsQueryService.salesSummary(resolveFrom(dateFrom), resolveTo(dateTo));
    }

    @GetMapping("/sales-summary/export")
    public ResponseEntity<byte[]> salesSummaryExport(
            @RequestParam(required = false) Instant dateFrom, @RequestParam(required = false) Instant dateTo) {
        Instant from = resolveFrom(dateFrom);
        Instant to = resolveTo(dateTo);
        return pdfResponse(reportPdfService.salesSummaryPdf(from, to), "sales-summary", to);
    }

    @GetMapping("/product-sales")
    public PageResponse<ProductSalesRow> productSales(
            @RequestParam(required = false) Instant dateFrom,
            @RequestParam(required = false) Instant dateTo,
            @RequestParam(required = false) Long productId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return PageResponse.of(
                reportsQueryService.productSales(resolveFrom(dateFrom), resolveTo(dateTo), productId, page, size));
    }

    @GetMapping("/product-sales/export")
    public ResponseEntity<byte[]> productSalesExport(
            @RequestParam(required = false) Instant dateFrom,
            @RequestParam(required = false) Instant dateTo,
            @RequestParam(required = false) Long productId) {
        Instant from = resolveFrom(dateFrom);
        Instant to = resolveTo(dateTo);
        return pdfResponse(reportPdfService.productSalesPdf(from, to, productId), "product-sales", to);
    }

    @GetMapping("/category-sales")
    public PageResponse<CategorySalesRow> categorySales(
            @RequestParam(required = false) Instant dateFrom,
            @RequestParam(required = false) Instant dateTo,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return PageResponse.of(
                reportsQueryService.categorySales(resolveFrom(dateFrom), resolveTo(dateTo), categoryId, page, size));
    }

    @GetMapping("/category-sales/export")
    public ResponseEntity<byte[]> categorySalesExport(
            @RequestParam(required = false) Instant dateFrom,
            @RequestParam(required = false) Instant dateTo,
            @RequestParam(required = false) Long categoryId) {
        Instant from = resolveFrom(dateFrom);
        Instant to = resolveTo(dateTo);
        return pdfResponse(reportPdfService.categorySalesPdf(from, to, categoryId), "category-sales", to);
    }

    @GetMapping("/customer-sales")
    public PageResponse<CustomerSalesRow> customerSales(
            @RequestParam(required = false) Instant dateFrom,
            @RequestParam(required = false) Instant dateTo,
            @RequestParam(required = false) Long customerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return PageResponse.of(
                reportsQueryService.customerSales(resolveFrom(dateFrom), resolveTo(dateTo), customerId, page, size));
    }

    @GetMapping("/customer-sales/export")
    public ResponseEntity<byte[]> customerSalesExport(
            @RequestParam(required = false) Instant dateFrom,
            @RequestParam(required = false) Instant dateTo,
            @RequestParam(required = false) Long customerId) {
        Instant from = resolveFrom(dateFrom);
        Instant to = resolveTo(dateTo);
        return pdfResponse(reportPdfService.customerSalesPdf(from, to, customerId), "customer-sales", to);
    }

    @GetMapping("/stock")
    public PageResponse<VariantInventoryRow> stock(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long productId,
            @RequestParam(required = false) StockStatus stockStatus,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return PageResponse.of(reportsQueryService.stock(categoryId, productId, stockStatus, page, size));
    }

    @GetMapping("/stock/export")
    public ResponseEntity<byte[]> stockExport(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long productId,
            @RequestParam(required = false) StockStatus stockStatus) {
        return pdfResponse(reportPdfService.stockPdf(categoryId, productId, stockStatus), "stock", Instant.now());
    }

    @GetMapping("/gst")
    public GstReportResponse gst(
            @RequestParam(required = false) Instant dateFrom,
            @RequestParam(required = false) Instant dateTo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return reportsQueryService.gst(resolveFrom(dateFrom), resolveTo(dateTo), page, size);
    }

    @GetMapping("/gst/export")
    public ResponseEntity<byte[]> gstExport(
            @RequestParam(required = false) Instant dateFrom, @RequestParam(required = false) Instant dateTo) {
        Instant from = resolveFrom(dateFrom);
        Instant to = resolveTo(dateTo);
        return pdfResponse(reportPdfService.gstPdf(from, to), "gst", to);
    }

    private Instant resolveFrom(Instant dateFrom) {
        return dateFrom != null ? dateFrom : Instant.now().minus(DEFAULT_RANGE_DAYS, ChronoUnit.DAYS);
    }

    private Instant resolveTo(Instant dateTo) {
        return dateTo != null ? dateTo : Instant.now();
    }

    private ResponseEntity<byte[]> pdfResponse(byte[] pdfBytes, String reportType, Instant asOf) {
        String filename = reportType + "-" + FILENAME_DATE.format(asOf.atZone(ZoneId.systemDefault())) + ".pdf";
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(pdfBytes);
    }
}
