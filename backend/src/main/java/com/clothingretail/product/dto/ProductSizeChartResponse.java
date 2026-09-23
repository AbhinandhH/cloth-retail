package com.clothingretail.product.dto;

import com.clothingretail.masterdata.dto.SizeChartRowResponse;
import java.util.List;

/** The public, customer-facing shape of a product's assigned size chart - no audit fields. */
public record ProductSizeChartResponse(Long id, String name, List<String> columns, List<SizeChartRowResponse> rows) {}
