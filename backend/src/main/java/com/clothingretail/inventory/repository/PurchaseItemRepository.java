package com.clothingretail.inventory.repository;

import com.clothingretail.inventory.PurchaseItem;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PurchaseItemRepository extends JpaRepository<PurchaseItem, Long> {
    List<PurchaseItem> findByPurchaseId(Long purchaseId);

    boolean existsByProductVariantIdIn(List<Long> productVariantIds);
}
