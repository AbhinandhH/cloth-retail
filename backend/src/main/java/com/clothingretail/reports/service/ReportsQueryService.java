package com.clothingretail.reports.service;

import com.clothingretail.inventory.StockStatus;
import com.clothingretail.reports.dto.CategorySalesRow;
import com.clothingretail.reports.dto.CustomerSalesRow;
import com.clothingretail.reports.dto.GstReportResponse;
import com.clothingretail.reports.dto.ProductSalesRow;
import com.clothingretail.reports.dto.SalesSummaryResponse;
import com.clothingretail.inventory.dto.VariantInventoryRow;
import java.time.Instant;
import org.springframework.data.domain.Page;

/**
 * Read-side of the admin Reports module. Every method here re-derives its data from the same
 * source tables the rest of the app already trusts (Order/OrderItem/ProductVariant) rather than
 * a separate reporting store - see the individual OrderRepository query doc comments for how each
 * one extends an existing dashboard/customers/inventory query.
 */
public interface ReportsQueryService {

    SalesSummaryResponse salesSummary(Instant dateFrom, Instant dateTo);

    Page<ProductSalesRow> productSales(Instant dateFrom, Instant dateTo, Long productId, int page, int size);

    Page<CategorySalesRow> categorySales(Instant dateFrom, Instant dateTo, Long categoryId, int page, int size);

    Page<CustomerSalesRow> customerSales(Instant dateFrom, Instant dateTo, Long customerId, int page, int size);

    Page<VariantInventoryRow> stock(Long categoryId, Long productId, StockStatus stockStatus, int page, int size);

    GstReportResponse gst(Instant dateFrom, Instant dateTo, int page, int size);
}
