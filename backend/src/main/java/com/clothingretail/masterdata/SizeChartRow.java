package com.clothingretail.masterdata;

import com.clothingretail.common.BaseEntity;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * One size's row of measurements within a {@link SizeChart} - a free-text {@code sizeLabel}
 * (not an FK to the global {@link Size} table, since a chart's rows are whatever labels the
 * admin entered, e.g. "42" for a footwear chart) plus {@code values}, positionally aligned to
 * the parent chart's {@code columns} list.
 */
@Entity
@Table(name = "size_chart_rows")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class SizeChartRow extends BaseEntity {

    @EqualsAndHashCode.Exclude
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "size_chart_id", nullable = false)
    private SizeChart sizeChart;

    @Column(name = "size_label", nullable = false, length = 50)
    private String sizeLabel;

    @Column(name = "display_order", nullable = false)
    private int displayOrder = 0;

    /** values.get(i) is this row's value for the parent chart's columns.get(i). */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "size_chart_row_values", joinColumns = @JoinColumn(name = "size_chart_row_id"))
    @OrderColumn(name = "value_order")
    @Column(name = "value_text", length = 255)
    private List<String> values = new ArrayList<>();
}
