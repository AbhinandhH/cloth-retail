package com.clothingretail.order.repository;

import com.clothingretail.order.Order;
import com.clothingretail.order.OrderItem;
import com.clothingretail.order.OrderStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderRepository extends JpaRepository<Order, Long>, JpaSpecificationExecutor<Order> {

    /**
     * JOIN FETCH items eagerly: {@code OrderService.createOrder}'s idempotency-key lookup (both
     * the fast path and the DataIntegrityViolationException race-loss path) calls this from
     * outside any transaction - by the time the result is mapped to a response DTO, this
     * repository call's own short-lived session has already closed, so {@code order.getItems()}
     * (a LAZY collection by default) would throw LazyInitializationException unless it was
     * already fully loaded during this one query.
     */
    @Query("SELECT o FROM Order o LEFT JOIN FETCH o.items WHERE o.idempotencyKey = :idempotencyKey")
    Optional<Order> findByIdempotencyKey(@Param("idempotencyKey") String idempotencyKey);

    Optional<Order> findByIdAndCustomerProfileId(Long id, Long customerProfileId);

    Page<Order> findByCustomerProfileId(Long customerProfileId, Pageable pageable);

    /**
     * Customer-facing order list: excludes a PENDING_PAYMENT order whose stock hasn't actually
     * been reserved yet (deferred-mode draft the customer hasn't clicked "Pay" on) - see
     * Order.stockReserved and SiteConfiguration.reserveStockOnlyAtPayment. A no-op filter for
     * every order created under the default (non-deferred) behaviour, since those are always
     * reserved immediately. {@link #findByIdAndCustomerProfileId} is deliberately NOT filtered
     * the same way - the customer must still be able to open their own not-yet-reserved draft by
     * id (the Payment page needs this before the customer has clicked Pay).
     */
    @Query("SELECT o FROM Order o WHERE o.customerProfile.id = :customerProfileId "
            + "AND NOT (o.status = :pendingStatus AND o.stockReserved = false)")
    Page<Order> findVisibleByCustomerProfileId(
            @Param("customerProfileId") Long customerProfileId,
            @Param("pendingStatus") OrderStatus pendingStatus,
            Pageable pageable);

    /**
     * Atomic status transition - a single conditional UPDATE, not a JPA load-then-save, mirroring
     * {@code ProductVariantRepository.reserveStock}'s own documented pattern. Used by
     * {@code PaymentServiceImpl.initiate} so two concurrent initiate calls for the same order
     * (two tabs, a slipped double-click) can't both pass a stale in-memory status check before
     * either commits - only one caller's transition succeeds; the other gets 0 rows affected and
     * must treat that as a hard failure, not a silent no-op.
     */
    @Modifying
    @Query("UPDATE Order o SET o.status = :to WHERE o.id = :id AND o.status = :from")
    int transitionStatus(@Param("id") Long id, @Param("from") OrderStatus from, @Param("to") OrderStatus to);

    List<Order> findByStatusAndReservationExpiresAtBefore(OrderStatus status, Instant instant);

    long countByStatus(OrderStatus status);

    /**
     * Powers the admin landing dashboard's "today's sales" figure and its sales-overview chart -
     * see AdminDashboardQueryService, which buckets these by day in Java rather than in SQL (the
     * date-truncation function differs between MySQL and the H2-in-MySQL-mode test profile, and
     * a couple of weeks of orders is a small enough set that bucketing in Java is simpler and
     * fully portable). {@code statuses} is the fixed SALE_STATUSES list there, not caller input.
     */
    List<Order> findByCreatedAtGreaterThanEqualAndStatusIn(Instant createdAt, List<OrderStatus> statuses);

    /**
     * Batched item-count lookup for the admin order list/dashboard rows: one query for a whole
     * page of order ids, instead of touching {@code order.getItems()} (a LAZY collection) once
     * per row, which would be N+1. See AdminOrderQueryService.
     */
    @Query("SELECT oi.order.id AS orderId, COALESCE(SUM(oi.quantity), 0) AS itemCount "
            + "FROM OrderItem oi WHERE oi.order.id IN :orderIds GROUP BY oi.order.id")
    List<OrderItemCountProjection> sumItemCountsByOrderIds(@Param("orderIds") List<Long> orderIds);

    /**
     * Top-selling products for the admin landing dashboard, aggregated from OrderItem's own
     * denormalized snapshot columns (not the live Product/ProductVariant) so a since-renamed or
     * deleted product's past sales still show up correctly. Grouped strictly by sku (the true
     * unique key) with MAX(...) picking a representative name/image, rather than grouping by all
     * three columns - two order snapshots for the same sku could in principle carry a
     * (since-corrected) different productName/imageUrl, which would otherwise split one product's
     * sales into multiple rows.
     */
    @Query("SELECT oi.sku AS sku, MAX(oi.productName) AS productName, MAX(oi.imageUrl) AS imageUrl, "
            + "SUM(oi.quantity) AS quantitySold, SUM(oi.lineTotal) AS revenue "
            + "FROM OrderItem oi WHERE oi.order.status IN :statuses "
            + "GROUP BY oi.sku ORDER BY SUM(oi.quantity) DESC")
    List<TopSellingProjection> findTopSellingProducts(@Param("statuses") List<OrderStatus> statuses, Pageable pageable);

    /**
     * Batched order-count + lifetime-spend lookup for the admin Customers module: one query for
     * a whole page of customer ids (the list view), or a singleton list (the detail view) -
     * never a per-row query, same batching principle as sumItemCountsByOrderIds above. A
     * customer with zero *counted* orders simply won't appear in the result set, so callers must
     * default to 0/ZERO for any id missing from the returned map. Restricted to {@link
     * SaleOrderStatuses#SALE_STATUSES} - an abandoned/failed-payment checkout attempt shouldn't
     * inflate "total amount spent" with money that was never actually charged.
     */
    @Query("SELECT o.customerProfile.id AS customerProfileId, COUNT(o) AS orderCount, COALESCE(SUM(o.totalAmount), 0) AS totalSpent "
            + "FROM Order o WHERE o.customerProfile.id IN :customerProfileIds AND o.status IN :statuses GROUP BY o.customerProfile.id")
    List<CustomerOrderStatsProjection> sumStatsByCustomerProfileIds(
            @Param("customerProfileIds") List<Long> customerProfileIds, @Param("statuses") List<OrderStatus> statuses);

    interface CustomerOrderStatsProjection {
        Long getCustomerProfileId();

        Long getOrderCount();

        BigDecimal getTotalSpent();
    }

    interface OrderItemCountProjection {
        Long getOrderId();

        Long getItemCount();
    }

    interface TopSellingProjection {
        String getSku();

        String getProductName();

        String getImageUrl();

        Long getQuantitySold();

        BigDecimal getRevenue();
    }
}
