package com.clothingretail.inventory.repository;

import com.clothingretail.inventory.Purchase;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PurchaseRepository extends JpaRepository<Purchase, Long> {
    long countByVendorId(Long vendorId);
}
