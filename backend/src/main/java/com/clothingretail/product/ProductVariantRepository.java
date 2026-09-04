package com.clothingretail.product;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

public interface ProductVariantRepository extends JpaRepository<ProductVariant, Long>, JpaSpecificationExecutor<ProductVariant> {
    List<ProductVariant> findByProductId(Long productId);

    Optional<ProductVariant> findBySku(String sku);

    boolean existsBySkuIgnoreCase(String sku);

    /** Sum of computed available quantity (stock - reserved - damaged, floored at 0) across all variants. */
    @Query("SELECT COALESCE(SUM(CASE WHEN (v.stockQuantity - v.reservedQuantity - v.damagedQuantity) > 0 "
            + "THEN (v.stockQuantity - v.reservedQuantity - v.damagedQuantity) ELSE 0 END), 0) FROM ProductVariant v")
    long sumAvailableQuantity();

    @Query("SELECT COALESCE(SUM(v.damagedQuantity), 0) FROM ProductVariant v")
    long sumDamagedQuantity();
}
