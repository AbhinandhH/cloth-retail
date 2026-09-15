package com.clothingretail.inventory.service;

import com.clothingretail.inventory.dto.PurchaseRequest;
import com.clothingretail.inventory.dto.PurchaseResponse;
import java.util.List;

/**
 * Creating a Purchase is the one place stock increases: it writes the
 * Purchase+PurchaseItem rows, bumps each target ProductVariant.stockQuantity,
 * and appends a PURCHASE_IN InventoryTransaction per line - all in one
 * transaction so the audit log and the live stock count can never drift
 * apart.
 */
public interface PurchaseService {

    PurchaseResponse createPurchase(PurchaseRequest request);

    PurchaseResponse getPurchase(Long id);

    List<PurchaseResponse> listPurchases();
}
