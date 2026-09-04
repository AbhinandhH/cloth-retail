package com.clothingretail.inventory;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PurchaseRepository extends JpaRepository<Purchase, Long> {
    long countByVendorId(Long vendorId);
}
