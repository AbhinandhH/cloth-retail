package com.clothingretail.inventory.dto;

import com.clothingretail.product.ProductStatus;
import java.time.Instant;

public record RecentProductRow(Long id, String name, String slug, ProductStatus status, Instant createdAt) {}
