package com.clothingretail.inventory;

/** Append-only audit log entry types. Positive-quantity IN types increase stock, OUT types decrease it. */
public enum InventoryTransactionType {
    PURCHASE_IN,
    SALE_OUT,
    RETURN_IN,
    ADJUSTMENT,
    CANCEL_REVERSAL
}
