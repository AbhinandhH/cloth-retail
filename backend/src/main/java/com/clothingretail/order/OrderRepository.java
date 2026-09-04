package com.clothingretail.order;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderRepository extends JpaRepository<Order, Long> {

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

    List<Order> findByStatusAndReservationExpiresAtBefore(OrderStatus status, Instant instant);
}
