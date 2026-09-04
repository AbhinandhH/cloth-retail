package com.clothingretail.product;

import com.clothingretail.common.BaseEntity;
import com.clothingretail.masterdata.Color;
import com.clothingretail.masterdata.Size;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "product_variants")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class ProductVariant extends BaseEntity {

    @EqualsAndHashCode.Exclude
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false, unique = true, length = 64)
    private String sku;

    // EAGER: tiny reference tables read in DTO mapping outside any open transaction.
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "size_id", nullable = false)
    private Size size;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "color_id", nullable = false)
    private Color color;

    @Column(name = "selling_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal sellingPrice;

    @Column(name = "discount_percent", nullable = false, precision = 5, scale = 2)
    private BigDecimal discountPercent = BigDecimal.ZERO;

    @Column(name = "stock_quantity", nullable = false)
    private int stockQuantity = 0;

    /** Brand new field, not derived from anything - the vendor's cost for this specific variant. */
    @Column(name = "cost_price", precision = 10, scale = 2)
    private BigDecimal costPrice;

    /** Nothing writes this yet (cart/checkout reservation doesn't exist) - that's correct for now. */
    @Column(name = "reserved_quantity", nullable = false)
    private int reservedQuantity = 0;

    /** Increased only by {@code StockService.recordDamage} - never reduces stockQuantity. */
    @Column(name = "damaged_quantity", nullable = false)
    private int damagedQuantity = 0;

    /** Null means "use the system default of 5" wherever it's evaluated. */
    @Column(name = "low_stock_threshold")
    private Integer lowStockThreshold;

    @Version
    @Column(nullable = false)
    private Long version;

    @Column(nullable = false)
    private boolean active = true;

    @EqualsAndHashCode.Exclude
    @OneToMany(mappedBy = "productVariant", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("displayOrder ASC")
    private List<ProductImage> images = new ArrayList<>();

    public void addImage(ProductImage image) {
        image.setProductVariant(this);
        images.add(image);
    }

    /** Computed, never persisted: the physical count minus what's reserved and what's damaged. */
    public int getAvailableQuantity() {
        return Math.max(0, stockQuantity - reservedQuantity - damagedQuantity);
    }
}
