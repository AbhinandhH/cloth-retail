package com.clothingretail.product.repository;

import com.clothingretail.product.ProductImage;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductImageRepository extends JpaRepository<ProductImage, Long> {
    List<ProductImage> findByProductColorMediaIdOrderByDisplayOrderAsc(Long productColorMediaId);
}
