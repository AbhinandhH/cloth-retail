package com.clothingretail.product.repository;

import com.clothingretail.product.Product;
import com.clothingretail.product.ProductStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {
    Optional<Product> findBySlugAndStatus(String slug, ProductStatus status);

    /** Powers the customer wishlist page - see ProductService.listByIds, which re-orders these
     * back into the caller's requested id order (this query's own result order isn't
     * guaranteed) and drops any id that no longer resolves to an active product. */
    List<Product> findByIdInAndStatus(List<Long> ids, ProductStatus status);

    boolean existsBySlugIgnoreCase(String slug);

    long countByCategoryId(Long categoryId);

    long countBySubCategoryId(Long subCategoryId);

    long countByBrandId(Long brandId);

    long countByMaterialId(Long materialId);
}
