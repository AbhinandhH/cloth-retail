package com.clothingretail.product.repository;

import com.clothingretail.product.ProductColorMedia;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductColorMediaRepository extends JpaRepository<ProductColorMedia, Long> {
    Optional<ProductColorMedia> findByProductIdAndColorId(Long productId, Long colorId);
}
