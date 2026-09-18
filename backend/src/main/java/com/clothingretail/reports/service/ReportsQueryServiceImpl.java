package com.clothingretail.reports.service;

import com.clothingretail.inventory.StockStatus;
import com.clothingretail.inventory.dto.VariantInventoryRow;
import com.clothingretail.inventory.service.InventoryQueryService;
import com.clothingretail.order.Order;
import com.clothingretail.order.OrderStatus;
import com.clothingretail.order.SaleOrderStatuses;
import com.clothingretail.order.repository.OrderRepository;
import com.clothingretail.order.repository.OrderRepository.CategorySalesProjection;
import com.clothingretail.order.repository.OrderRepository.CustomerSalesProjection;
import com.clothingretail.order.repository.OrderRepository.GstTotalsProjection;
import com.clothingretail.order.repository.OrderRepository.TopSellingProjection;
import com.clothingretail.reports.dto.CategorySalesRow;
import com.clothingretail.reports.dto.CustomerSalesRow;
import com.clothingretail.reports.dto.GstReportResponse;
import com.clothingretail.reports.dto.GstReportRow;
import com.clothingretail.reports.dto.ProductSalesRow;
import com.clothingretail.reports.dto.SalesSummaryResponse;
import com.clothingretail.reports.dto.SalesSummaryRow;
import com.clothingretail.common.PageResponse;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@Log4j2
public class ReportsQueryServiceImpl implements ReportsQueryService {

    private static final List<OrderStatus> SALE_STATUSES = SaleOrderStatuses.SALE_STATUSES;
    private static final ZoneId ZONE = ZoneId.systemDefault();

    private final OrderRepository orderRepository;
    private final InventoryQueryService inventoryQueryService;

    public ReportsQueryServiceImpl(OrderRepository orderRepository, InventoryQueryService inventoryQueryService) {
        this.orderRepository = orderRepository;
        this.inventoryQueryService = inventoryQueryService;
    }

    @Override
    public SalesSummaryResponse salesSummary(Instant dateFrom, Instant dateTo) {
        log.info("[1900] Building sales summary dateFrom={}, dateTo={}", dateFrom, dateTo);
        List<Order> orders = orderRepository.findByCreatedAtBetweenAndStatusIn(dateFrom, dateTo, SALE_STATUSES);

        LocalDate start = dateFrom.atZone(ZONE).toLocalDate();
        LocalDate end = dateTo.atZone(ZONE).toLocalDate();
        // Pre-seed every day in the range with zero so a quiet day still renders as a point,
        // same reasoning as AdminDashboardQueryServiceImpl's sales-overview chart.
        Map<LocalDate, BigDecimal> revenueByDay = new LinkedHashMap<>();
        Map<LocalDate, Long> countByDay = new LinkedHashMap<>();
        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
            revenueByDay.put(d, BigDecimal.ZERO);
            countByDay.put(d, 0L);
        }

        BigDecimal totalRevenue = BigDecimal.ZERO;
        for (Order order : orders) {
            LocalDate day = order.getCreatedAt().atZone(ZONE).toLocalDate();
            revenueByDay.merge(day, order.getTotalAmount(), BigDecimal::add);
            countByDay.merge(day, 1L, Long::sum);
            totalRevenue = totalRevenue.add(order.getTotalAmount());
        }

        List<SalesSummaryRow> rows = revenueByDay.entrySet().stream()
                .map(e -> new SalesSummaryRow(e.getKey(), countByDay.get(e.getKey()), e.getValue()))
                .toList();

        long totalOrders = orders.size();
        BigDecimal avgOrderValue = totalOrders == 0
                ? BigDecimal.ZERO
                : totalRevenue.divide(BigDecimal.valueOf(totalOrders), 2, RoundingMode.HALF_UP);

        log.info("[1901] Sales summary built: days={}, totalOrders={}, totalRevenue={}", rows.size(), totalOrders, totalRevenue);
        return new SalesSummaryResponse(rows, totalRevenue, totalOrders, avgOrderValue);
    }

    @Override
    public Page<ProductSalesRow> productSales(Instant dateFrom, Instant dateTo, Long productId, int page, int size) {
        log.info("[1902] Building product sales report dateFrom={}, dateTo={}, productId={}, page={}, size={}",
                dateFrom, dateTo, productId, page, size);
        Page<TopSellingProjection> result =
                orderRepository.findProductSalesReport(SALE_STATUSES, dateFrom, dateTo, productId, PageRequest.of(page, size));
        return result.map(r -> new ProductSalesRow(r.getSku(), r.getProductName(), r.getImageUrl(), r.getQuantitySold(), r.getRevenue()));
    }

    @Override
    public Page<CategorySalesRow> categorySales(Instant dateFrom, Instant dateTo, Long categoryId, int page, int size) {
        log.info("[1903] Building category sales report dateFrom={}, dateTo={}, categoryId={}, page={}, size={}",
                dateFrom, dateTo, categoryId, page, size);
        Page<CategorySalesProjection> result = orderRepository.findCategorySalesReport(
                SALE_STATUSES, dateFrom, dateTo, categoryId, PageRequest.of(page, size));
        return result.map(r -> new CategorySalesRow(r.getCategoryName(), r.getQuantitySold(), r.getRevenue()));
    }

    @Override
    public Page<CustomerSalesRow> customerSales(Instant dateFrom, Instant dateTo, Long customerId, int page, int size) {
        log.info("[1904] Building customer sales report dateFrom={}, dateTo={}, customerId={}, page={}, size={}",
                dateFrom, dateTo, customerId, page, size);
        Page<CustomerSalesProjection> result = orderRepository.findCustomerSalesReport(
                SALE_STATUSES, dateFrom, dateTo, customerId, PageRequest.of(page, size));
        return result.map(r -> new CustomerSalesRow(
                r.getCustomerProfileId(), r.getFullName(), r.getEmail(), r.getOrderCount(), r.getTotalSpent()));
    }

    @Override
    public Page<VariantInventoryRow> stock(Long categoryId, Long productId, StockStatus stockStatus, int page, int size) {
        log.info("[1905] Building stock report categoryId={}, productId={}, stockStatus={}, page={}, size={}",
                categoryId, productId, stockStatus, page, size);
        return inventoryQueryService.listVariants(
                null, categoryId, productId, null, null, stockStatus, null, "sku", "asc", page, size);
    }

    @Override
    public GstReportResponse gst(Instant dateFrom, Instant dateTo, int page, int size) {
        log.info("[1906] Building GST report dateFrom={}, dateTo={}, page={}, size={}", dateFrom, dateTo, page, size);
        Page<Order> orders =
                orderRepository.findByStatusInAndCreatedAtBetween(SALE_STATUSES, dateFrom, dateTo, PageRequest.of(page, size));
        Page<GstReportRow> rows = orders.map(o -> new GstReportRow(
                o.getOrderNumber(), o.getCreatedAt(), o.getCgstAmount(), o.getSgstAmount(), o.getTotalAmount()));
        GstTotalsProjection totals = orderRepository.sumGstTotals(SALE_STATUSES, dateFrom, dateTo);
        return new GstReportResponse(PageResponse.of(rows), totals.getTotalCgst(), totals.getTotalSgst(), totals.getTotalRevenue());
    }
}
