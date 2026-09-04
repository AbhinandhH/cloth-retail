package com.clothingretail.inventory;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface InventoryTransactionRepository
        extends JpaRepository<InventoryTransaction, Long>, JpaSpecificationExecutor<InventoryTransaction> {
    List<InventoryTransaction> findByProductVariantId(Long productVariantId);

    List<InventoryTransaction> findByReferenceTypeAndReferenceId(String referenceType, Long referenceId);

    boolean existsByProductVariantIdIn(List<Long> productVariantIds);
}
