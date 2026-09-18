package com.clothingretail.reports.service;

import com.clothingretail.inventory.StockStatus;
import com.clothingretail.inventory.dto.VariantInventoryRow;
import com.clothingretail.reports.dto.CategorySalesRow;
import com.clothingretail.reports.dto.CustomerSalesRow;
import com.clothingretail.reports.dto.GstReportResponse;
import com.clothingretail.reports.dto.GstReportRow;
import com.clothingretail.reports.dto.ProductSalesRow;
import com.clothingretail.reports.dto.SalesSummaryResponse;
import com.clothingretail.reports.dto.SalesSummaryRow;
import com.clothingretail.siteconfig.SiteConfiguration;
import com.clothingretail.siteconfig.repository.SiteConfigurationRepository;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.List;
import java.util.Locale;
import lombok.extern.log4j.Log4j2;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Entities.EscapeMode;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

/**
 * Renders every report as a PDF via one shared template (templates/reports/report.html):
 * Thymeleaf produces the HTML, jsoup normalizes it into strict XHTML (openhtmltopdf's own XML
 * parser is brittle against real-world markup - unescaped entities, minor malformed spots from
 * dynamic data), then openhtmltopdf converts that XHTML+CSS into PDF bytes. See the pom.xml
 * comment above the three dependencies this relies on for the license/version rationale.
 *
 * "Export" always pulls the full matching result set (capped at {@link #EXPORT_ROW_LIMIT}), not
 * just the page currently on screen - an export means "everything matching the filter."
 */
@Service
@Log4j2
public class ReportPdfServiceImpl implements ReportPdfService {

    private static final int EXPORT_ROW_LIMIT = 2000;
    private static final ZoneId ZONE = ZoneId.systemDefault();
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM);
    private static final DateTimeFormatter TIMESTAMP_FMT = DateTimeFormatter.ofPattern("d MMM yyyy, h:mm a");

    private final ReportsQueryService reportsQueryService;
    private final SiteConfigurationRepository siteConfigurationRepository;
    private final TemplateEngine templateEngine;

    public ReportPdfServiceImpl(
            ReportsQueryService reportsQueryService,
            SiteConfigurationRepository siteConfigurationRepository,
            TemplateEngine templateEngine) {
        this.reportsQueryService = reportsQueryService;
        this.siteConfigurationRepository = siteConfigurationRepository;
        this.templateEngine = templateEngine;
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] salesSummaryPdf(Instant dateFrom, Instant dateTo) {
        log.info("[1910] Rendering sales summary PDF dateFrom={}, dateTo={}", dateFrom, dateTo);
        SalesSummaryResponse summary = reportsQueryService.salesSummary(dateFrom, dateTo);
        List<List<String>> rows = summary.rows().stream()
                .map(r -> List.of(DATE_FMT.format(r.date()), String.valueOf(r.orderCount()), currency(r.revenue())))
                .toList();
        List<String> totals = List.of("Total", String.valueOf(summary.totalOrders()), currency(summary.totalRevenue()));
        ReportPdfModel model = buildModel(
                "Sales Summary",
                dateRangeSummary(dateFrom, dateTo),
                List.of("Date", "Orders", "Revenue"),
                rows,
                totals,
                false);
        return render(model);
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] productSalesPdf(Instant dateFrom, Instant dateTo, Long productId) {
        log.info("[1911] Rendering product sales PDF dateFrom={}, dateTo={}, productId={}", dateFrom, dateTo, productId);
        Page<ProductSalesRow> page = reportsQueryService.productSales(dateFrom, dateTo, productId, 0, EXPORT_ROW_LIMIT);
        List<List<String>> rows = page.getContent().stream()
                .map(r -> List.of(r.sku(), r.productName(), String.valueOf(r.quantitySold()), currency(r.revenue())))
                .toList();
        BigDecimal totalRevenue = page.getContent().stream().map(ProductSalesRow::revenue).reduce(BigDecimal.ZERO, BigDecimal::add);
        long totalUnits = page.getContent().stream().mapToLong(ProductSalesRow::quantitySold).sum();
        List<String> totals = List.of("Total", "", String.valueOf(totalUnits), currency(totalRevenue));
        ReportPdfModel model = buildModel(
                "Product-wise Sales",
                dateRangeSummary(dateFrom, dateTo),
                List.of("SKU", "Product", "Units sold", "Revenue"),
                rows,
                totals,
                page.getTotalElements() > EXPORT_ROW_LIMIT);
        return render(model);
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] categorySalesPdf(Instant dateFrom, Instant dateTo, Long categoryId) {
        log.info("[1912] Rendering category sales PDF dateFrom={}, dateTo={}, categoryId={}", dateFrom, dateTo, categoryId);
        Page<CategorySalesRow> page = reportsQueryService.categorySales(dateFrom, dateTo, categoryId, 0, EXPORT_ROW_LIMIT);
        List<List<String>> rows = page.getContent().stream()
                .map(r -> List.of(r.categoryName(), String.valueOf(r.quantitySold()), currency(r.revenue())))
                .toList();
        BigDecimal totalRevenue = page.getContent().stream().map(CategorySalesRow::revenue).reduce(BigDecimal.ZERO, BigDecimal::add);
        long totalUnits = page.getContent().stream().mapToLong(CategorySalesRow::quantitySold).sum();
        List<String> totals = List.of("Total", String.valueOf(totalUnits), currency(totalRevenue));
        ReportPdfModel model = buildModel(
                "Category-wise Sales",
                dateRangeSummary(dateFrom, dateTo),
                List.of("Category", "Units sold", "Revenue"),
                rows,
                totals,
                page.getTotalElements() > EXPORT_ROW_LIMIT);
        return render(model);
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] customerSalesPdf(Instant dateFrom, Instant dateTo, Long customerId) {
        log.info("[1913] Rendering customer sales PDF dateFrom={}, dateTo={}, customerId={}", dateFrom, dateTo, customerId);
        Page<CustomerSalesRow> page = reportsQueryService.customerSales(dateFrom, dateTo, customerId, 0, EXPORT_ROW_LIMIT);
        List<List<String>> rows = page.getContent().stream()
                .map(r -> List.of(r.fullName(), r.email(), String.valueOf(r.orderCount()), currency(r.totalSpent())))
                .toList();
        BigDecimal totalSpent = page.getContent().stream().map(CustomerSalesRow::totalSpent).reduce(BigDecimal.ZERO, BigDecimal::add);
        long totalOrders = page.getContent().stream().mapToLong(CustomerSalesRow::orderCount).sum();
        List<String> totals = List.of("Total", "", String.valueOf(totalOrders), currency(totalSpent));
        ReportPdfModel model = buildModel(
                "Customer-wise Sales",
                dateRangeSummary(dateFrom, dateTo),
                List.of("Customer", "Email", "Orders", "Total spent"),
                rows,
                totals,
                page.getTotalElements() > EXPORT_ROW_LIMIT);
        return render(model);
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] stockPdf(Long categoryId, Long productId, StockStatus stockStatus) {
        log.info("[1914] Rendering stock PDF categoryId={}, productId={}, stockStatus={}", categoryId, productId, stockStatus);
        Page<VariantInventoryRow> page =
                reportsQueryService.stock(categoryId, productId, stockStatus, 0, EXPORT_ROW_LIMIT);
        List<List<String>> rows = page.getContent().stream()
                .map(r -> List.of(
                        r.sku(),
                        r.productName(),
                        r.categoryName(),
                        r.colorName() + " / " + r.sizeName(),
                        String.valueOf(r.stockQuantity()),
                        String.valueOf(r.reservedQuantity()),
                        String.valueOf(r.damagedQuantity()),
                        String.valueOf(r.availableQuantity())))
                .toList();
        String title = stockStatus == StockStatus.LOW_STOCK || stockStatus == StockStatus.OUT_OF_STOCK
                ? "Low Stock Report"
                : "Stock Report";
        ReportPdfModel model = buildModel(
                title,
                "Generated for current inventory" + (stockStatus != null ? " - filter: " + stockStatus : ""),
                List.of("SKU", "Product", "Category", "Variant", "Stock", "Reserved", "Damaged", "Available"),
                rows,
                null,
                page.getTotalElements() > EXPORT_ROW_LIMIT);
        return render(model);
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] gstPdf(Instant dateFrom, Instant dateTo) {
        log.info("[1915] Rendering GST PDF dateFrom={}, dateTo={}", dateFrom, dateTo);
        GstReportResponse full = fullGstReport(dateFrom, dateTo);
        List<List<String>> rows = full.page().content().stream()
                .map(r -> List.of(
                        r.orderNumber(),
                        DATE_FMT.format(r.createdAt().atZone(ZONE).toLocalDate()),
                        currency(r.cgstAmount()),
                        currency(r.sgstAmount()),
                        currency(r.totalAmount())))
                .toList();
        List<String> totals =
                List.of("Total", "", currency(full.totalCgst()), currency(full.totalSgst()), currency(full.totalRevenue()));
        ReportPdfModel model = buildModel(
                "GST / Tax Report",
                dateRangeSummary(dateFrom, dateTo),
                List.of("Order", "Date", "CGST", "SGST", "Total"),
                rows,
                totals,
                full.page().totalElements() > EXPORT_ROW_LIMIT);
        return render(model);
    }

    private GstReportResponse fullGstReport(Instant dateFrom, Instant dateTo) {
        // gst() already returns grand totals independent of the page size, so a single
        // EXPORT_ROW_LIMIT-sized call gives both the (capped) row list and the true totals.
        return reportsQueryService.gst(dateFrom, dateTo, 0, EXPORT_ROW_LIMIT);
    }

    private ReportPdfModel buildModel(
            String title,
            String filterSummary,
            List<String> headers,
            List<List<String>> rows,
            List<String> totalsRow,
            boolean truncated) {
        String businessName = siteConfigurationRepository.findById(SiteConfiguration.SINGLETON_ID)
                .map(SiteConfiguration::getBusinessName)
                .orElse("Loom Atelier Studio");
        String generatedAt = TIMESTAMP_FMT.format(Instant.now().atZone(ZONE));
        return new ReportPdfModel(businessName, title, filterSummary, generatedAt, headers, rows, totalsRow, truncated);
    }

    private byte[] render(ReportPdfModel model) {
        Context context = new Context();
        context.setVariable("model", model);
        String html = templateEngine.process("reports/report", context);

        // jsoup normalization: openhtmltopdf's XML parser rejects real-world HTML (unescaped
        // entities, boolean attributes) that Thymeleaf's own XHTML mode doesn't fully guarantee
        // away once dynamic report data flows through - this is the standard defensive step for
        // this stack, not optional. See pom.xml's comment on these three dependencies.
        Document jsoupDoc = Jsoup.parse(html);
        jsoupDoc.outputSettings().syntax(Document.OutputSettings.Syntax.xml).escapeMode(EscapeMode.xhtml);
        String xhtml = jsoupDoc.html();

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.withHtmlContent(xhtml, null);
            builder.toStream(out);
            builder.run();
            log.info("[1916] Rendered PDF: {} bytes", out.size());
            return out.toByteArray();
        } catch (Exception e) {
            log.error("[1917] PDF rendering failed: {}", e.getMessage(), e);
            throw new IllegalStateException("Failed to render report PDF", e);
        }
    }

    private String dateRangeSummary(Instant dateFrom, Instant dateTo) {
        return DATE_FMT.format(dateFrom.atZone(ZONE).toLocalDate()) + " - " + DATE_FMT.format(dateTo.atZone(ZONE).toLocalDate());
    }

    /**
     * "Rs." rather than the on-screen "₹" symbol: openhtmltopdf's built-in PDF-standard
     * fonts (no custom font is registered) don't carry the Indian Rupee glyph, so rendering it
     * silently produced a "#" placeholder instead - this sidesteps that rather than requiring a
     * bundled custom font just to draw one character. Digit grouping still matches the frontend's
     * en-IN Intl.NumberFormat (lakhs/crores), only the symbol differs.
     */
    private String currency(BigDecimal amount) {
        NumberFormat format = NumberFormat.getIntegerInstance(Locale.forLanguageTag("en-IN"));
        BigDecimal value = (amount == null ? BigDecimal.ZERO : amount).setScale(0, RoundingMode.HALF_UP);
        return "Rs. " + format.format(value);
    }
}
