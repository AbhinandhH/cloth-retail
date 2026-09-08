package com.clothingretail.dashboard.dto;

import java.math.BigDecimal;

/** One row of the dashboard's top-selling-products list, aggregated from OrderItem snapshots by SKU - see OrderRepository.findTopSellingProducts. */
public record TopSellingProductRow(
        String sku, String productName, String imageUrl, long quantitySold, BigDecimal revenue) {}
