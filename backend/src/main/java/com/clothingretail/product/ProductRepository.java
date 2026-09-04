package com.clothingretail.product;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {
    Optional<Product> findBySlugAndStatus(String slug, ProductStatus status);

    boolean existsBySlugIgnoreCase(String slug);

    long countByCategoryId(Long categoryId);

    long countBySubCategoryId(Long subCategoryId);

    long countByBrandId(Long brandId);

    long countByMaterialId(Long materialId);
}
