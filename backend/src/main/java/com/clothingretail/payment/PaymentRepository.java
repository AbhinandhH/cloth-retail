package com.clothingretail.payment;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByGatewayReference(String gatewayReference);

    Optional<Payment> findByWebhookEventId(String webhookEventId);

    /** Most recently created payment attempt for an order, if any - used to report Order.paymentStatus. */
    Optional<Payment> findFirstByOrderIdOrderByIdDesc(Long orderId);

    /**
     * Batched "latest payment per order" lookup for the admin order list/dashboard rows: one
     * query for a whole page of order ids (each order effectively only ever has one payment
     * attempt in this codebase, but this is written to be correct even if that changes), instead
     * of one {@link #findFirstByOrderIdOrderByIdDesc} call per row - which would be N+1. See
     * AdminOrderQueryService.
     */
    @Query("SELECT p FROM Payment p WHERE p.id IN "
            + "(SELECT MAX(p2.id) FROM Payment p2 WHERE p2.order.id IN :orderIds GROUP BY p2.order.id)")
    List<Payment> findLatestByOrderIds(@Param("orderIds") List<Long> orderIds);

    /**
     * Atomically claims this payment for processing: only succeeds while it's still PENDING, so
     * two concurrent deliveries of the same (or a merely overlapping) webhook can't both apply
     * their stock-mutating side effects. Returns 1 if this call won the race, 0 if some other
     * call already resolved the payment first - the caller re-reads the row in that case.
     */
    // clearAutomatically: this bulk UPDATE bypasses the persistence context entirely, so without
    // clearing it, a later findById(sameId) in this same transaction would return the
    // already-managed (now stale) Payment instance straight from the first-level cache instead
    // of re-reading it - see PaymentWebhookService.handleWebhook, which always re-fetches by id
    // right after calling this.
    @Modifying(clearAutomatically = true)
    @Query("UPDATE Payment p SET p.status = :status, p.webhookEventId = :webhookEventId, p.gatewayPaymentId = COALESCE(:gatewayPaymentId, p.gatewayPaymentId) WHERE p.id = :id AND p.status = :expectedCurrentStatus")
    int markProcessed(
            @Param("id") Long id,
            @Param("status") PaymentStatus status,
            @Param("webhookEventId") String webhookEventId,
            @Param("expectedCurrentStatus") PaymentStatus expectedCurrentStatus,
            @Param("gatewayPaymentId") String gatewayPaymentId);
}
