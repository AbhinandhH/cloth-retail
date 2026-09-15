package com.clothingretail.inventory.repository;

import com.clothingretail.inventory.DamageRecord;
import com.clothingretail.inventory.InventoryTransaction;
import com.clothingretail.inventory.InventoryTransactionType;
import com.clothingretail.inventory.StockStatus;
import com.clothingretail.product.ProductStatus;
import com.clothingretail.product.ProductVariant;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

/** Builds filter predicates for the admin inventory listings, mirroring {@code ProductSpecifications}' style. */
public final class InventorySpecifications {

    /** Applied wherever {@code lowStockThreshold} is null - "use the system default of 5". */
    public static final int DEFAULT_LOW_STOCK_THRESHOLD = 5;

    private InventorySpecifications() {}

    public static Specification<ProductVariant> filterVariants(
            String q, Long categoryId, Long colorId, Long sizeId, StockStatus stockStatus, ProductStatus productStatus) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (categoryId != null) {
                predicates.add(cb.equal(root.get("product").get("category").get("id"), categoryId));
            }
            if (colorId != null) {
                predicates.add(cb.equal(root.get("color").get("id"), colorId));
            }
            if (sizeId != null) {
                predicates.add(cb.equal(root.get("size").get("id"), sizeId));
            }
            if (productStatus != null) {
                predicates.add(cb.equal(root.get("product").get("status"), productStatus));
            }
            if (q != null && !q.isBlank()) {
                String pattern = "%" + q.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("sku")), pattern),
                        cb.like(cb.lower(root.get("product").get("name")), pattern)));
            }
            if (stockStatus != null) {
                predicates.add(stockStatusPredicate(root, cb, stockStatus));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private static Predicate stockStatusPredicate(Root<ProductVariant> root, CriteriaBuilder cb, StockStatus stockStatus) {
        Expression<Integer> available = cb.diff(
                cb.diff(root.get("stockQuantity"), root.get("reservedQuantity")), root.get("damagedQuantity"));
        Expression<Integer> threshold = cb.coalesce(root.get("lowStockThreshold"), DEFAULT_LOW_STOCK_THRESHOLD);

        return switch (stockStatus) {
            case OUT_OF_STOCK -> cb.lessThanOrEqualTo(available, 0);
            case LOW_STOCK -> cb.and(cb.greaterThan(available, 0), cb.lessThanOrEqualTo(available, threshold));
            case IN_STOCK -> cb.greaterThan(available, threshold);
        };
    }

    public static Specification<InventoryTransaction> filterTransactions(
            Long variantId, InventoryTransactionType type, Instant dateFrom, Instant dateTo) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (variantId != null) {
                predicates.add(cb.equal(root.get("productVariant").get("id"), variantId));
            }
            if (type != null) {
                predicates.add(cb.equal(root.get("type"), type));
            }
            if (dateFrom != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), dateFrom));
            }
            if (dateTo != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), dateTo));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    public static Specification<DamageRecord> filterDamages(Long variantId) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (variantId != null) {
                predicates.add(cb.equal(root.get("productVariant").get("id"), variantId));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    /** Same computed bucket as {@link #filterVariants}, evaluated in Java for row mapping. */
    public static StockStatus computeStockStatus(ProductVariant variant) {
        int available = variant.getAvailableQuantity();
        int threshold = variant.getLowStockThreshold() != null ? variant.getLowStockThreshold() : DEFAULT_LOW_STOCK_THRESHOLD;
        if (available <= 0) {
            return StockStatus.OUT_OF_STOCK;
        }
        return available <= threshold ? StockStatus.LOW_STOCK : StockStatus.IN_STOCK;
    }
}
