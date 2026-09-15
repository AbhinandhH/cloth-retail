package com.clothingretail.product.repository;

import com.clothingretail.product.Product;
import com.clothingretail.product.ProductStatus;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

/**
 * Builds the AND-combined product listing filter. Each filter dimension uses
 * its own join to the variants collection so e.g. "sizeId=1" and "colorId=2"
 * each mean "the product has *some* matching variant" rather than requiring
 * a single variant to satisfy every filter at once - the more useful
 * behaviour for a storefront facet UI. {@code distinct(true)} avoids
 * duplicate rows when a product has multiple matching variants.
 */
public final class ProductSpecifications {

    private ProductSpecifications() {}

    public static Specification<Product> filter(
            Long categoryId,
            Long subCategoryId,
            Long sizeId,
            Long colorId,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            String q,
            boolean activeOnly) {
        return (root, query, cb) -> {
            query.distinct(true);
            List<Predicate> predicates = new ArrayList<>();

            if (activeOnly) {
                predicates.add(cb.equal(root.get("status"), ProductStatus.ACTIVE));
            }
            if (categoryId != null) {
                predicates.add(cb.equal(root.get("category").get("id"), categoryId));
            }
            if (subCategoryId != null) {
                predicates.add(cb.equal(root.get("subCategory").get("id"), subCategoryId));
            }
            if (sizeId != null) {
                Join<Object, Object> join = root.join("variants", JoinType.INNER);
                predicates.add(cb.equal(join.get("size").get("id"), sizeId));
            }
            if (colorId != null) {
                Join<Object, Object> join = root.join("variants", JoinType.INNER);
                predicates.add(cb.equal(join.get("color").get("id"), colorId));
            }
            if (minPrice != null) {
                Join<Object, Object> join = root.join("variants", JoinType.INNER);
                predicates.add(cb.greaterThanOrEqualTo(join.get("sellingPrice"), minPrice));
            }
            if (maxPrice != null) {
                Join<Object, Object> join = root.join("variants", JoinType.INNER);
                predicates.add(cb.lessThanOrEqualTo(join.get("sellingPrice"), maxPrice));
            }
            if (q != null && !q.isBlank()) {
                String pattern = "%" + q.trim().toLowerCase() + "%";
                Join<Object, Object> join = root.join("variants", JoinType.LEFT);
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("name")), pattern),
                        cb.like(cb.lower(join.get("sku")), pattern)));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    /** Filter dimensions for the admin product listing - deliberately simpler than the storefront filter above. */
    public static Specification<Product> adminFilter(String q, Long categoryId, ProductStatus status) {
        return (root, query, cb) -> {
            query.distinct(true);
            List<Predicate> predicates = new ArrayList<>();

            if (categoryId != null) {
                predicates.add(cb.equal(root.get("category").get("id"), categoryId));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (q != null && !q.isBlank()) {
                String pattern = "%" + q.trim().toLowerCase() + "%";
                Join<Object, Object> join = root.join("variants", JoinType.LEFT);
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("name")), pattern),
                        cb.like(cb.lower(join.get("sku")), pattern)));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
