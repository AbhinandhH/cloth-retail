package com.clothingretail.masterdata;

import com.clothingretail.common.AuditableMasterEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A reusable, admin-managed measurement table (e.g. "Men's Tops - Standard": columns
 * Chest/Waist/Length, one row per size) assignable to any number of {@link
 * com.clothingretail.product.Product}s via {@code Product.sizeChart} - the same
 * one-master-many-products shape as {@link Brand}/{@link Material}, not a per-product copy.
 * Unlike {@link SizeGroup} (which only computes which {@link Size}s are pickable for a
 * category), a chart's columns are free-text and fully admin-defined, since different
 * garment types measure different things (a footwear chart might have just "Foot length",
 * a tops chart Chest/Shoulder/Sleeve/Length) - there is deliberately no fixed schema.
 */
@Entity
@Table(name = "size_charts")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class SizeChart extends AuditableMasterEntity {

    @Column(nullable = false, unique = true, length = 150)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "display_order", nullable = false)
    private int displayOrder = 0;

    @Column(nullable = false)
    private boolean active = true;

    /**
     * Ordered measurement column labels (e.g. ["Chest", "Waist", "Length"]) - a plain
     * {@code @ElementCollection}, not a full entity, since a column is nothing more than an
     * ordered label with no further behavior. Every row's {@code values} list must be the
     * same length, in the same order, as this list - position N in a row's values is that
     * row's value for column N here.
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "size_chart_columns", joinColumns = @JoinColumn(name = "size_chart_id"))
    @OrderColumn(name = "column_order")
    @Column(name = "column_value", nullable = false, length = 100)
    private List<String> columns = new ArrayList<>();

    @EqualsAndHashCode.Exclude
    @OneToMany(mappedBy = "sizeChart", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("displayOrder ASC")
    private List<SizeChartRow> rows = new ArrayList<>();

    public void addRow(SizeChartRow row) {
        row.setSizeChart(this);
        rows.add(row);
    }
}
