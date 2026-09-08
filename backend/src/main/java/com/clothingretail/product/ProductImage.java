package com.clothingretail.product;

import com.clothingretail.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.Comparator;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Media (image or video) belonging to a variant (not the product) since color determines the
 * media set. Despite the class name it can hold a video too - see {@link MediaType} - kept as
 * ProductImage rather than renamed to avoid churning every existing call site for what is still,
 * by row count, almost entirely images.
 */
@Entity
@Table(name = "product_images")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class ProductImage extends BaseEntity {

    @EqualsAndHashCode.Exclude
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_variant_id", nullable = false)
    private ProductVariant productVariant;

    @Column(nullable = false, length = 500)
    private String url;

    @Column(name = "display_order", nullable = false)
    private int displayOrder = 0;

    // Column is is_primary, not primary - PRIMARY is a MySQL reserved word.
    @Column(name = "is_primary", nullable = false)
    private boolean primary = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "media_type", nullable = false, length = 10)
    private MediaType mediaType = MediaType.IMAGE;

    /** Primary image first, then by displayOrder - the single ordering every gallery/thumbnail call site should use. */
    public static Comparator<ProductImage> displayOrderComparator() {
        return Comparator.comparing(ProductImage::isPrimary).reversed().thenComparing(ProductImage::getDisplayOrder);
    }
}
