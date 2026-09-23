package com.clothingretail.order.service;

import com.clothingretail.common.ConflictException;
import com.clothingretail.common.NotFoundException;
import com.clothingretail.customer.CustomerProfile;
import com.clothingretail.customer.repository.CustomerProfileRepository;
import com.clothingretail.order.Order;
import com.clothingretail.order.OrderItem;
import com.clothingretail.order.SaleOrderStatuses;
import com.clothingretail.order.repository.OrderRepository;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

/** Same Thymeleaf -> jsoup -> openhtmltopdf pipeline as {@code reports.service.ReportPdfServiceImpl.render} - see that class's doc comment for why the jsoup normalization step exists. */
@Service
@Log4j2
public class OrderInvoiceServiceImpl implements OrderInvoiceService {

    private static final ZoneId ZONE = ZoneId.systemDefault();
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM);
    private static final DateTimeFormatter TIMESTAMP_FMT = DateTimeFormatter.ofPattern("d MMM yyyy, h:mm a");

    private final OrderRepository orderRepository;
    private final CustomerProfileRepository customerProfileRepository;
    private final SiteConfigurationRepository siteConfigurationRepository;
    private final TemplateEngine templateEngine;

    public OrderInvoiceServiceImpl(
            OrderRepository orderRepository,
            CustomerProfileRepository customerProfileRepository,
            SiteConfigurationRepository siteConfigurationRepository,
            TemplateEngine templateEngine) {
        this.orderRepository = orderRepository;
        this.customerProfileRepository = customerProfileRepository;
        this.siteConfigurationRepository = siteConfigurationRepository;
        this.templateEngine = templateEngine;
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] renderAdminInvoice(Long orderId) {
        log.info("[1960] Rendering admin invoice orderId={}", orderId);
        Order order = orderRepository.findById(orderId).orElseThrow(() -> {
            log.error("[1961] Invoice failed, order not found orderId={}", orderId);
            return new NotFoundException("Order not found: " + orderId);
        });
        return render(order);
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] renderCustomerInvoice(Long userId, Long orderId) {
        log.info("[1962] Rendering customer invoice orderId={} userId={}", orderId, userId);
        CustomerProfile profile = customerProfileRepository.findByUserId(userId).orElseThrow(() -> {
            log.error("[1963] Invoice failed, no customer profile for userId={}", userId);
            return new NotFoundException("Customer profile not found");
        });
        Order order = orderRepository.findByIdAndCustomerProfileId(orderId, profile.getId()).orElseThrow(() -> {
            log.error("[1964] Invoice failed, order not found orderId={} for userId={}", orderId, userId);
            return new NotFoundException("Order not found: " + orderId);
        });
        return render(order);
    }

    private byte[] render(Order order) {
        if (!SaleOrderStatuses.SALE_STATUSES.contains(order.getStatus())) {
            log.error("[1965] Invoice unavailable, order not yet paid orderId={} status={}", order.getId(), order.getStatus());
            throw new ConflictException("Invoice isn't available until payment is confirmed");
        }

        SiteConfiguration config = siteConfigurationRepository.findById(SiteConfiguration.SINGLETON_ID)
                .orElseThrow(() -> new IllegalStateException(
                        "Singleton site_configuration row (id=1) is missing - this is a startup-time misconfiguration"));

        var buyerUser = order.getCustomerProfile().getUser();
        List<InvoiceLineItemRow> items = order.getItems().stream().map(this::toLineItem).toList();

        InvoicePdfModel model = new InvoicePdfModel(
                config.getBusinessName(),
                config.getGstin(),
                config.getRegisteredAddress(),
                order.getOrderNumber(),
                DATE_FMT.format(order.getCreatedAt().atZone(ZONE).toLocalDate()),
                buyerUser.getFullName(),
                buyerUser.getEmail(),
                buyerUser.getMobileNumber(),
                order.getShippingAddressLine1(),
                order.getShippingAddressLine2(),
                order.getShippingCity() + ", " + order.getShippingState() + " " + order.getShippingPostalCode(),
                order.getShippingCountry(),
                items,
                currency(order.getSubtotal()),
                currency(order.getDiscountTotal()),
                currency(order.getShippingCharge()),
                "CGST (" + order.getCgstPercent() + "%)",
                currency(order.getCgstAmount()),
                "SGST (" + order.getSgstPercent() + "%)",
                currency(order.getSgstAmount()),
                currency(order.getTotalAmount()),
                TIMESTAMP_FMT.format(Instant.now().atZone(ZONE)));

        return renderPdf(model);
    }

    private InvoiceLineItemRow toLineItem(OrderItem item) {
        return new InvoiceLineItemRow(
                item.getProductName(),
                item.getSku(),
                item.getColorName() + " / " + item.getSizeName(),
                item.getQuantity(),
                currency(item.getUnitPrice()),
                currency(item.getLineTotal()));
    }

    private byte[] renderPdf(InvoicePdfModel model) {
        Context context = new Context();
        context.setVariable("model", model);
        String html = templateEngine.process("invoices/order-invoice", context);

        Document jsoupDoc = Jsoup.parse(html);
        jsoupDoc.outputSettings().syntax(Document.OutputSettings.Syntax.xml).escapeMode(EscapeMode.xhtml);
        String xhtml = jsoupDoc.html();

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.withHtmlContent(xhtml, null);
            builder.toStream(out);
            builder.run();
            log.info("[1966] Rendered invoice PDF: {} bytes", out.size());
            return out.toByteArray();
        } catch (Exception e) {
            log.error("[1967] Invoice PDF rendering failed: {}", e.getMessage(), e);
            throw new IllegalStateException("Failed to render invoice PDF", e);
        }
    }

    /** Same "Rs." convention as ReportPdfServiceImpl.currency - no custom font is registered for the PDF renderer, so it has no Rupee glyph to draw. */
    private String currency(BigDecimal amount) {
        NumberFormat format = NumberFormat.getIntegerInstance(Locale.forLanguageTag("en-IN"));
        BigDecimal value = (amount == null ? BigDecimal.ZERO : amount).setScale(0, RoundingMode.HALF_UP);
        return "Rs. " + format.format(value);
    }
}
