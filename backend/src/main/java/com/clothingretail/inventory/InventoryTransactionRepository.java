package com.clothingretail.inventory;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InventoryTransactionRepository extends JpaRepository<InventoryTransaction, Long> {
    List<InventoryTransaction> findByProductVariantId(Long productVariantId);

    List<InventoryTransaction> findByReferenceTypeAndReferenceId(String referenceType, Long referenceId);
}
