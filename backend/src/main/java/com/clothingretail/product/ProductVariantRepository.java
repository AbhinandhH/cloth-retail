package com.clothingretail.product;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductVariantRepository extends JpaRepository<ProductVariant, Long> {
    List<ProductVariant> findByProductId(Long productId);

    Optional<ProductVariant> findBySku(String sku);

    boolean existsBySkuIgnoreCase(String sku);
}
