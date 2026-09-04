package com.clothingretail.masterdata;

import com.clothingretail.common.BaseEntity;
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
 * A single (SizeGroup, Size) membership plus the extra {@code displayOrder} attribute -
 * a many-to-many-with-payload join, modeled as its own entity (same technique as
 * {@code inventory.PurchaseItem}) rather than a bare {@code @ManyToMany} since the ordering
 * needs somewhere to live and the same {@link Size} row can be reused across multiple
 * groups with a different position in each.
 */
@Entity
@Table(name = "size_group_sizes")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class SizeGroupSize extends BaseEntity {

    @EqualsAndHashCode.Exclude
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "size_group_id", nullable = false)
    private SizeGroup sizeGroup;

    // EAGER: tiny reference table read in DTO mapping outside any open transaction.
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "size_id", nullable = false)
    private Size size;

    @Column(name = "display_order", nullable = false)
    private int displayOrder = 0;
}
