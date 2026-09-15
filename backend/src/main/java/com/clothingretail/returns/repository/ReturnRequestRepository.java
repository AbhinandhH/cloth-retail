package com.clothingretail.returns.repository;

import com.clothingretail.returns.ReturnRequest;
import com.clothingretail.returns.ReturnRequestStatus;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReturnRequestRepository extends JpaRepository<ReturnRequest, Long>, JpaSpecificationExecutor<ReturnRequest> {

    Optional<ReturnRequest> findByIdAndCustomerProfileId(Long id, Long customerProfileId);

    Page<ReturnRequest> findByCustomerProfileId(Long customerProfileId, Pageable pageable);

    /** Duplicate-active-request check: true if any request on this item hasn't been rejected yet (PENDING/APPROVED/REFUNDED all count as active). */
    boolean existsByOrderItemIdAndStatusNot(Long orderItemId, ReturnRequestStatus status);

    /** The active (non-rejected) request for an item, if any - at most one can exist, enforced by the check above at creation time. Powers "see the status of my existing request" on a later page visit. */
    Optional<ReturnRequest> findFirstByOrderItemIdAndStatusNot(Long orderItemId, ReturnRequestStatus status);

    boolean existsByEvidenceReferenceCode(String evidenceReferenceCode);

    /**
     * Row-locked read used by every state-changing admin action (approve/reject/refund) - closes
     * the double-submit race a plain read-then-write status check would leave open. See
     * ReturnAdminServiceImpl.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM ReturnRequest r WHERE r.id = :id")
    Optional<ReturnRequest> findByIdForUpdate(@Param("id") Long id);
}
