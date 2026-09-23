package com.clothingretail.order.service;

/**
 * Renders a GST invoice PDF for a single completed order, reusing the same Thymeleaf +
 * openhtmltopdf pipeline {@code ReportPdfServiceImpl} already uses for reports (see
 * templates/invoices/order-invoice.html). An order's own snapshotted tax columns
 * (cgstPercent/cgstAmount/sgstPercent/sgstAmount) are read as-is - no live recomputation against
 * today's TaxSettings, so an invoice always reflects the rate that was active when that order was
 * placed.
 */
public interface OrderInvoiceService {

    /** No ownership check beyond the controller's own role gate - any admin/employee with ORDERS access may fetch any order's invoice. */
    byte[] renderAdminInvoice(Long orderId);

    /** Enforces that {@code orderId} belongs to the customer identified by {@code userId} - a customer must never be able to fetch another customer's invoice by guessing an id. */
    byte[] renderCustomerInvoice(Long userId, Long orderId);
}
