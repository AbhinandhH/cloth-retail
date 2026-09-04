package com.clothingretail.inventory;

import com.clothingretail.auth.User;
import com.clothingretail.common.BaseEntity;
import com.clothingretail.product.ProductVariant;
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
 * Append-only audit log of every stock movement. The live count lives on
 * {@link ProductVariant#getStockQuantity()} (or {@link ProductVariant#getDamagedQuantity()}
 * for {@code DAMAGE} rows) - this table never duplicates it as a separate "Inventory"
 * entity, it only records how it changed. Immutable: no update/delete endpoint, ever.
 */
@Entity
@Table(name = "inventory_transactions")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class InventoryTransaction extends BaseEntity {

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "product_variant_id", nullable = false)
    private ProductVariant productVariant;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private InventoryTransactionType type;

    /** Magnitude of the change (always positive - {@code type} says which direction). */
    @Column(nullable = false)
    private int quantity;

    @Column(name = "reference_type", length = 50)
    private String referenceType;

    @Column(name = "reference_id")
    private Long referenceId;

    @Column(columnDefinition = "TEXT")
    private String reason;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "performed_by")
    private User performedBy;

    /**
     * Before/after snapshot of whichever counter this transaction type affects:
     * stockQuantity for PURCHASE_IN/SALE_OUT/RETURN_IN/ADJUSTMENT/CANCEL_REVERSAL,
     * damagedQuantity for DAMAGE.
     */
    @Column(name = "previous_quantity", nullable = false)
    private int previousQuantity;

    @Column(name = "new_quantity", nullable = false)
    private int newQuantity;
}
