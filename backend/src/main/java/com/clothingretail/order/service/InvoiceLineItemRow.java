package com.clothingretail.order.service;

record InvoiceLineItemRow(
        String productName, String sku, String colorSize, int quantity, String unitPrice, String lineTotal) {}
