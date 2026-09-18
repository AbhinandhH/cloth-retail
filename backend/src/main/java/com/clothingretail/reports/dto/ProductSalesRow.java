package com.clothingretail.reports.dto;

import java.math.BigDecimal;

public record ProductSalesRow(String sku, String productName, String imageUrl, long quantitySold, BigDecimal revenue) {}
