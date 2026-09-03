package com.clothingretail.product;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductImageRepository extends JpaRepository<ProductImage, Long> {
    List<ProductImage> findByProductVariantIdOrderByDisplayOrderAsc(Long productVariantId);
}
