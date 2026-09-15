package com.clothingretail.returns;

import com.clothingretail.common.BaseEntity;
import com.clothingretail.customer.CustomerProfile;
import com.clothingretail.order.Order;
import com.clothingretail.order.OrderItem;
import com.clothingretail.payment.Refund;
import com.clothingretail.product.ProductVariant;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * One entity covers both SIZE_EXCHANGE and DAMAGED_PRODUCT requests via {@link #requestType} -
 * they share order/item/customer linkage, status, reason, and admin-review fields; the
 * type-specific fields below are simply unused on the branch that doesn't need them (the same
 * nullable/branch-dependent pattern {@link OrderItem#getProductVariant()} already uses). No
 * separate history table: {@code updatedAt} (from BaseEntity) plus {@link #adminNote}/
 * {@link #reviewedBy}/{@link #reviewedAt} is the same one-shot-audit-row approach
 * {@code DamageRecord} already uses at this app's scale.
 */
@Entity
@Table(name = "return_requests")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class ReturnRequest extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_item_id", nullable = false)
    private OrderItem orderItem;

    /** Denormalized for direct customer-scoped ownership queries, mirrors Order.customerProfile. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_profile_id", nullable = false)
    private CustomerProfile customerProfile;

    @Enumerated(EnumType.STRING)
    @Column(name = "request_type", nullable = false, length = 20)
    private RequestType requestType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReturnRequestStatus status = ReturnRequestStatus.PENDING;

    @Column(columnDefinition = "TEXT")
    private String reason;

    // --- SIZE_EXCHANGE only ---

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requested_variant_id")
    private ProductVariant requestedVariant;

    /** Snapshot, same philosophy as OrderItem.sizeName - survives the variant later being deleted. */
    @Column(name = "requested_size_name", length = 20)
    private String requestedSizeName;

    // --- DAMAGED_PRODUCT only ---

    @Enumerated(EnumType.STRING)
    @Column(name = "evidence_status", nullable = false, length = 20)
    private EvidenceStatus evidenceStatus = EvidenceStatus.NOT_SUBMITTED;

    /** WhatsApp correlation token shown to the customer - see WhatsAppService. */
    @Column(name = "evidence_reference_code", length = 20, unique = true)
    private String evidenceReferenceCode;

    /** UUID-based filename under the private return-evidence directory - never the original filename. */
    @Column(name = "evidence_filename", length = 255)
    private String evidenceFilename;

    @Column(name = "evidence_content_type", length = 100)
    private String evidenceContentType;

    @Column(name = "evidence_uploaded_at")
    private Instant evidenceUploadedAt;

    // --- admin review, both types ---

    @Column(name = "admin_note", columnDefinition = "TEXT")
    private String adminNote;

    /** Acting admin's user id - a plain column, not a FK, mirrors OrderNote.createdBy. */
    @Column(name = "reviewed_by")
    private Long reviewedBy;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    // --- refund linkage, DAMAGED_PRODUCT only, set exactly once ---

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "refund_id", unique = true)
    private Refund refund;
}
