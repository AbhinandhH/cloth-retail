package com.clothingretail.payment;

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
    @Query("UPDATE Payment p SET p.status = :status, p.webhookEventId = :webhookEventId WHERE p.id = :id AND p.status = :expectedCurrentStatus")
    int markProcessed(
            @Param("id") Long id,
            @Param("status") PaymentStatus status,
            @Param("webhookEventId") String webhookEventId,
            @Param("expectedCurrentStatus") PaymentStatus expectedCurrentStatus);
}
