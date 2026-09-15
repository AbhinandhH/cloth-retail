package com.clothingretail.dashboard.service;

import com.clothingretail.dashboard.dto.AdminDashboardResponse;
import com.clothingretail.dashboard.dto.SalesOverviewPoint;
import com.clothingretail.dashboard.dto.TopSellingProductRow;
import com.clothingretail.inventory.InventoryQueryService;
import com.clothingretail.inventory.dto.DashboardResponse;
import com.clothingretail.order.Order;
import com.clothingretail.order.OrderRepository;
import com.clothingretail.order.OrderRepository.TopSellingProjection;
import com.clothingretail.order.OrderStatus;
import com.clothingretail.order.SaleOrderStatuses;
import com.clothingretail.order.AdminOrderQueryService;
import com.clothingretail.order.dto.AdminOrderDashboardResponse;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Read-side aggregation for the admin landing dashboard (Today's sales, Total/Pending orders,
 * stock breakdown, recent orders, a sales-over-time chart, and top-selling products).
 *
 * Deliberately reuses {@link AdminOrderQueryService#dashboard()} and {@link
 * InventoryQueryService#dashboard()} for everything they already compute (total/pending orders,
 * recent orders, in-stock/low-stock/out-of-stock counts) instead of re-querying those same counts
 * a second way here - see AdminDashboardResponse's own doc comment for why. Only "today's sales",
 * the sales-overview trend, and top-selling products are genuinely new aggregates, added to
 * {@link OrderRepository} alongside its existing order-item aggregation query.
 */
@Service
@Transactional(readOnly = true)
@Log4j2
public class AdminDashboardQueryServiceImpl implements AdminDashboardQueryService {

    private static final int SALES_OVERVIEW_DAYS = 14;
    private static final int TOP_SELLING_LIMIT = 5;

    // See SaleOrderStatuses' own doc comment - shared with the admin Customers module so
    // "total amount spent" per customer means the same thing as "today's sales" here.
    private static final List<OrderStatus> SALE_STATUSES = SaleOrderStatuses.SALE_STATUSES;

    private final OrderRepository orderRepository;
    private final AdminOrderQueryService adminOrderQueryService;
    private final InventoryQueryService inventoryQueryService;

    public AdminDashboardQueryServiceImpl(
            OrderRepository orderRepository,
            AdminOrderQueryService adminOrderQueryService,
            InventoryQueryService inventoryQueryService) {
        this.orderRepository = orderRepository;
        this.adminOrderQueryService = adminOrderQueryService;
        this.inventoryQueryService = inventoryQueryService;
    }

    @Override
    public AdminDashboardResponse dashboard() {
        log.info("[1500] Building admin landing dashboard");

        AdminOrderDashboardResponse orderDashboard = adminOrderQueryService.dashboard();
        DashboardResponse inventoryDashboard = inventoryQueryService.dashboard();
        // The three stock buckets (StockStatus: IN_STOCK/LOW_STOCK/OUT_OF_STOCK) are mutually
        // exclusive and exhaustive over all variants, so "in stock" (healthy stock, not just
        // "not zero") is exactly the remainder - keeps the three dashboard tiles summing to the
        // real total variant count instead of low-stock items double-counting as "in stock" too.
        long productsInStock = Math.max(
                0,
                inventoryDashboard.totalVariants()
                        - inventoryDashboard.lowStockCount()
                        - inventoryDashboard.outOfStockCount());

        ZoneId zone = ZoneId.systemDefault();
        LocalDate today = LocalDate.now(zone);
        Instant startOfToday = today.atStartOfDay(zone).toInstant();
        LocalDate overviewStart = today.minusDays(SALES_OVERVIEW_DAYS - 1L);
        Instant overviewSince = overviewStart.atStartOfDay(zone).toInstant();

        List<Order> saleOrders = orderRepository.findByCreatedAtGreaterThanEqualAndStatusIn(overviewSince, SALE_STATUSES);
        log.info("[1501] Loaded {} sale-status orders since {}", saleOrders.size(), overviewSince);

        // Pre-seed every day in the window with zero so a quiet day still renders as a point on
        // the chart rather than a gap.
        Map<LocalDate, BigDecimal> salesByDay = new LinkedHashMap<>();
        Map<LocalDate, Long> countByDay = new LinkedHashMap<>();
        for (LocalDate d = overviewStart; !d.isAfter(today); d = d.plusDays(1)) {
            salesByDay.put(d, BigDecimal.ZERO);
            countByDay.put(d, 0L);
        }

        BigDecimal todaysSales = BigDecimal.ZERO;
        long todaysOrderCount = 0;
        for (Order order : saleOrders) {
            LocalDate day = order.getCreatedAt().atZone(zone).toLocalDate();
            salesByDay.merge(day, order.getTotalAmount(), BigDecimal::add);
            countByDay.merge(day, 1L, Long::sum);
            if (!order.getCreatedAt().isBefore(startOfToday)) {
                todaysSales = todaysSales.add(order.getTotalAmount());
                todaysOrderCount++;
            }
        }

        List<SalesOverviewPoint> salesOverview = salesByDay.entrySet().stream()
                .map(e -> new SalesOverviewPoint(e.getKey(), e.getValue(), countByDay.get(e.getKey())))
                .toList();

        List<TopSellingProjection> topSellingRows =
                orderRepository.findTopSellingProducts(SALE_STATUSES, PageRequest.of(0, TOP_SELLING_LIMIT));
        List<TopSellingProductRow> topSellingProducts = topSellingRows.stream()
                .map(r -> new TopSellingProductRow(
                        r.getSku(), r.getProductName(), r.getImageUrl(), r.getQuantitySold(), r.getRevenue()))
                .toList();

        log.info(
                "[1502] Dashboard built: todaysSales={}, todaysOrders={}, salesOverviewDays={}, topSelling={}, productsInStock={}",
                todaysSales, todaysOrderCount, salesOverview.size(), topSellingProducts.size(), productsInStock);

        return new AdminDashboardResponse(
                todaysSales,
                todaysOrderCount,
                orderDashboard.totalOrders(),
                orderDashboard.pendingCount(),
                productsInStock,
                inventoryDashboard.lowStockCount(),
                inventoryDashboard.outOfStockCount(),
                orderDashboard.recentOrders(),
                salesOverview,
                topSellingProducts);
    }
}
