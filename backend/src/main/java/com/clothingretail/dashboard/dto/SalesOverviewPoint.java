package com.clothingretail.dashboard.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/** One day's worth of sales for the dashboard's trend chart. Days with no sales still get a point (sales=0), so the chart doesn't skip gaps. */
public record SalesOverviewPoint(LocalDate date, BigDecimal sales, long orderCount) {}
