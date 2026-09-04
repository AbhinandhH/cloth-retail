package com.clothingretail.inventory;

/** Computed (never persisted) bucket for a variant's available quantity vs. its low-stock threshold. */
public enum StockStatus {
    IN_STOCK,
    LOW_STOCK,
    OUT_OF_STOCK
}
