package com.clothingretail.product;

/**
 * Lifecycle status of a product. "Out of stock" is deliberately NOT a value here -
 * it's a computed, per-variant fact (see {@link ProductVariant#getAvailableQuantity()}),
 * never a product-level status.
 */
public enum ProductStatus {
    DRAFT,
    ACTIVE,
    INACTIVE,
    ARCHIVED
}
