package com.clothingretail.order;

import com.clothingretail.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Append-only audit log of every status change an order goes through, system-driven or
 * admin-driven alike - see {@link OrderTransitionService} (admin path) and the history-writing
 * calls added to {@code OrderCreationService}, {@code PaymentService}, {@code
 * PaymentWebhookService} and {@code OrderReservationCleanupJob} (system paths). {@link
 * #changedBy} is null for every system-driven row - that's how the admin UI distinguishes
 * "System" from a named admin in the order timeline. Immutable: no update/delete, ever, same
 * pattern as {@code InventoryTransaction}.
 */
@Entity
@Table(name = "order_status_history")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class OrderStatusHistory extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    /** Null only for the very first row (order creation) - see OrderCreationService. */
    @Enumerated(EnumType.STRING)
    @Column(name = "previous_status", length = 20)
    private OrderStatus previousStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "new_status", nullable = false, length = 20)
    private OrderStatus newStatus;

    /** Acting admin's user id - null for a system-driven transition. */
    @Column(name = "changed_by")
    private Long changedBy;

    @Column(columnDefinition = "TEXT")
    private String reason;
}
