package com.clothingretail.masterdata;

import com.clothingretail.common.AuditableMasterEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A named, ordered subset of the global {@link Size} list, associated with one or more
 * {@link Category}s. Used only to compute which sizes are valid to pick from for a given
 * category (see PublicSizeAvailabilityController) - a {@code ProductVariant.size} still
 * points directly at the flat global {@link Size} table, so removing a group can never
 * orphan anything and needs no delete-guard.
 */
@Entity
@Table(name = "size_groups")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class SizeGroup extends AuditableMasterEntity {

    @Column(nullable = false, length = 150)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "display_order", nullable = false)
    private int displayOrder = 0;

    @Column(nullable = false)
    private boolean active = true;

    @EqualsAndHashCode.Exclude
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "size_group_categories",
            joinColumns = @JoinColumn(name = "size_group_id"),
            inverseJoinColumns = @JoinColumn(name = "category_id"))
    private Set<Category> categories = new HashSet<>();

    @EqualsAndHashCode.Exclude
    @OneToMany(mappedBy = "sizeGroup", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("displayOrder ASC")
    private List<SizeGroupSize> sizeGroupSizes = new ArrayList<>();

    public void addSizeGroupSize(SizeGroupSize sizeGroupSize) {
        sizeGroupSize.setSizeGroup(this);
        sizeGroupSizes.add(sizeGroupSize);
    }
}
