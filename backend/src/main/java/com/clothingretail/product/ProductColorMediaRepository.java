package com.clothingretail.product;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductColorMediaRepository extends JpaRepository<ProductColorMedia, Long> {
    Optional<ProductColorMedia> findByProductIdAndColorId(Long productId, Long colorId);
}
