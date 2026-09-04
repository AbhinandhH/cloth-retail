package com.clothingretail.inventory;

import com.clothingretail.auth.User;
import com.clothingretail.common.BaseEntity;
import com.clothingretail.product.ProductVariant;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A single record of damaged stock. Marking damage does NOT reduce
 * {@link ProductVariant#getStockQuantity()} - it only increments
 * {@link ProductVariant#getDamagedQuantity()} (which reduces computed available stock).
 * {@code createdAt} (from {@link BaseEntity}) is the date/time reported.
 */
@Entity
@Table(name = "damage_records")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class DamageRecord extends BaseEntity {

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "product_variant_id", nullable = false)
    private ProductVariant productVariant;

    @Column(nullable = false)
    private int quantity;

    // EAGER: tiny reference table read in DTO mapping outside any open transaction.
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "damage_reason_id", nullable = false)
    private DamageReason reason;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "reported_by")
    private User reportedBy;
}
