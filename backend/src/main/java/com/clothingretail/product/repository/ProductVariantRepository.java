package com.clothingretail.product.repository;

import com.clothingretail.product.ProductVariant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductVariantRepository extends JpaRepository<ProductVariant, Long>, JpaSpecificationExecutor<ProductVariant> {
    List<ProductVariant> findByProductId(Long productId);

    Optional<ProductVariant> findBySku(String sku);

    boolean existsBySkuIgnoreCase(String sku);

    long countBySizeId(Long sizeId);

    long countByColorId(Long colorId);

    /** Sum of computed available quantity (stock - reserved - damaged, floored at 0) across all variants. */
    @Query("SELECT COALESCE(SUM(CASE WHEN (v.stockQuantity - v.reservedQuantity - v.damagedQuantity) > 0 "
            + "THEN (v.stockQuantity - v.reservedQuantity - v.damagedQuantity) ELSE 0 END), 0) FROM ProductVariant v")
    long sumAvailableQuantity();

    @Query("SELECT COALESCE(SUM(v.damagedQuantity), 0) FROM ProductVariant v")
    long sumDamagedQuantity();

    /**
     * Atomically reserves {@code qty} units for checkout - a single conditional UPDATE, not a
     * JPA load-then-save. The WHERE clause is both the availability check AND the concurrency
     * guard: MySQL/H2 take a row lock for the duration of this UPDATE, so two concurrent callers
     * reserving the same variant serialize on that lock, and the second one re-evaluates the
     * WHERE clause against the first one's already-applied change. Returns the number of rows
     * affected - 1 means the reservation succeeded, 0 means insufficient available stock. No
     * application-level lock/synchronized block anywhere; correctness (including across multiple
     * backend instances) comes entirely from the database's own row-level locking.
     */
    @Modifying
    @Query("UPDATE ProductVariant v SET v.reservedQuantity = v.reservedQuantity + :qty, v.version = v.version + 1 "
            + "WHERE v.id = :id AND (v.stockQuantity - v.reservedQuantity - v.damagedQuantity) >= :qty")
    int reserveStock(@Param("id") Long id, @Param("qty") int qty);

    /**
     * Atomically releases a previously-successful reservation of {@code qty} units (order
     * cancelled/expired/payment failed before ever decrementing stock) - no condition needed
     * since the caller only ever releases exactly what it knows was reserved.
     */
    @Modifying
    @Query("UPDATE ProductVariant v SET v.reservedQuantity = v.reservedQuantity - :qty, v.version = v.version + 1 "
            + "WHERE v.id = :id")
    int releaseReservation(@Param("id") Long id, @Param("qty") int qty);

    /**
     * Atomically fulfils a reservation on successful payment: the physical stock count drops by
     * {@code qty} and the matching reservation is released in the same statement (the unit was
     * reserved, now it's actually gone).
     */
    @Modifying
    @Query("UPDATE ProductVariant v SET v.stockQuantity = v.stockQuantity - :qty, "
            + "v.reservedQuantity = v.reservedQuantity - :qty, v.version = v.version + 1 "
            + "WHERE v.id = :id")
    int decrementStockOnSale(@Param("id") Long id, @Param("qty") int qty);

    /** Lightweight scalar read used to snapshot stockQuantity immediately around a decrement, for the audit trail. */
    @Query("SELECT v.stockQuantity FROM ProductVariant v WHERE v.id = :id")
    int getStockQuantity(@Param("id") Long id);

    /**
     * Atomically reverses a previously-fulfilled sale on order cancellation: the physical stock
     * count goes back up by {@code qty} - the mirror image of {@link #decrementStockOnSale}. Used
     * only when the order being cancelled had already reached a status where stock was actually
     * decremented (CONFIRMED/PROCESSING/PACKED) - see AdminOrderService. No condition needed,
     * same reasoning as {@link #releaseReservation}: the caller only ever restores exactly what
     * it knows this order's item previously took.
     */
    @Modifying
    @Query("UPDATE ProductVariant v SET v.stockQuantity = v.stockQuantity + :qty, v.version = v.version + 1 "
            + "WHERE v.id = :id")
    int restoreStockOnCancellation(@Param("id") Long id, @Param("qty") int qty);

    /**
     * Atomically decrements stock for an approved size-exchange replacement - conditional on
     * availability at approval time (stock could have moved since the customer's original
     * request), same locking/condition shape as {@link #reserveStock}. Returns 0 if insufficient
     * available stock, which the caller must treat as a hard failure, not a silent no-op. See
     * ReturnAdminServiceImpl#approveExchange.
     */
    @Modifying
    @Query("UPDATE ProductVariant v SET v.stockQuantity = v.stockQuantity - :qty, v.version = v.version + 1 "
            + "WHERE v.id = :id AND (v.stockQuantity - v.reservedQuantity - v.damagedQuantity) >= :qty")
    int decrementStockForExchange(@Param("id") Long id, @Param("qty") int qty);

    /**
     * Atomically restocks the original variant on an approved size-exchange - this app has no
     * separate "customer's returned item physically received" checkpoint, so approval is the
     * trust point (same as this app's existing lack of an RMA-receipt step anywhere else).
     * Mirror image of {@link #restoreStockOnCancellation}. See ReturnAdminServiceImpl#approveExchange.
     */
    @Modifying
    @Query("UPDATE ProductVariant v SET v.stockQuantity = v.stockQuantity + :qty, v.version = v.version + 1 "
            + "WHERE v.id = :id")
    int restockOnReturn(@Param("id") Long id, @Param("qty") int qty);

    /** Sibling variants (other sizes) of the same product+color - the data source for "only show sizes actually in stock" on a size-exchange request. */
    List<ProductVariant> findByProductIdAndColorId(Long productId, Long colorId);
}
