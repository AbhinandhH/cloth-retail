package com.clothingretail.payment;

import java.math.BigDecimal;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RefundRepository extends JpaRepository<Refund, Long> {
    /** A payment could in principle be refunded more than once (partial refunds) - the most recent one is what the admin detail view shows. */
    Optional<Refund> findFirstByPaymentIdOrderByIdDesc(Long paymentId);

    @Query("SELECT COALESCE(SUM(r.amount), 0) FROM Refund r WHERE r.payment.id = :paymentId AND r.status = com.clothingretail.payment.RefundStatus.COMPLETED")
    BigDecimal sumCompletedAmountByPaymentId(@Param("paymentId") Long paymentId);
}
