package com.clothingretail.masterdata.dto;

import java.util.List;

public record SizeChartRowResponse(Long id, String sizeLabel, List<String> values, int displayOrder) {}
