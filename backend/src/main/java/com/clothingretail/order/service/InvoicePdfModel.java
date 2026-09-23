package com.clothingretail.order.service;

import java.util.List;

/** Everything templates/invoices/order-invoice.html needs, pre-formatted as display strings - same "shape it, don't template-plumb" split as reports/service/ReportPdfModel. */
record InvoicePdfModel(
        String businessName,
        String gstin,
        String registeredAddress,
        String orderNumber,
        String orderDate,
        String buyerName,
        String buyerEmail,
        String buyerPhone,
        String shippingAddressLine1,
        String shippingAddressLine2,
        String shippingCityStatePostal,
        String shippingCountry,
        List<InvoiceLineItemRow> items,
        String subtotal,
        String discountTotal,
        String shippingCharge,
        String cgstLabel,
        String cgstAmount,
        String sgstLabel,
        String sgstAmount,
        String totalAmount,
        String generatedAt) {}
