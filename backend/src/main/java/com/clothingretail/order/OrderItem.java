package com.clothingretail.order;

import com.clothingretail.common.BaseEntity;
import com.clothingretail.product.ProductVariant;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Snapshot columns (productName/sku/colorName/sizeName/unitPrice/discountPercent/lineTotal) are
 * the source of truth for display - {@link #productVariant} is kept only for reference and is
 * nullable-safe (ON DELETE SET NULL): the order stays fully readable even after its variant is
 * deleted.
 */
@Entity
@Table(name = "order_items")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class OrderItem extends BaseEntity {

    @EqualsAndHashCode.Exclude
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_variant_id")
    private ProductVariant productVariant;

    @Column(name = "product_name", nullable = false, length = 200)
    private String productName;

    @Column(nullable = false, length = 64)
    private String sku;

    @Column(name = "color_name", nullable = false, length = 50)
    private String colorName;

    @Column(name = "size_name", nullable = false, length = 20)
    private String sizeName;

    @Column(nullable = false)
    private int quantity;

    @Column(name = "unit_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "discount_percent", nullable = false, precision = 5, scale = 2)
    private BigDecimal discountPercent = BigDecimal.ZERO;

    @Column(name = "line_total", nullable = false, precision = 10, scale = 2)
    private BigDecimal lineTotal;

    /** Snapshot of the variant's primary image URL at order-creation time - same resolution as ProductService's primary-image logic. Nullable: a variant may have no images. */
    @Column(name = "image_url", length = 500)
    private String imageUrl;

    /**
     * The cart_items row this line was created from - a soft reference (no FK) used only to
     * remove the matching cart line once payment succeeds (see
     * PaymentWebhookServiceImpl#applyOutcome), since OrderCreationService no longer clears the
     * cart at order-creation time. May end up pointing at an already-deleted row (the customer
     * removed it manually before payment completed, or a re-delivered webhook already cleaned it
     * up) - callers must tolerate that as a silent no-op, not an error.
     */
    @Column(name = "source_cart_item_id")
    private Long sourceCartItemId;
}
