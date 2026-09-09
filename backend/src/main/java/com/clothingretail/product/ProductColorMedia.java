package com.clothingretail.product;

import com.clothingretail.common.BaseEntity;
import com.clothingretail.masterdata.Color;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.ArrayList;
import java.util.List;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A product's shared photo/video set for one color, reused by every size variant of that color -
 * a garment looks the same regardless of size, it only fits differently, so images belong here
 * rather than to any individual {@link ProductVariant}. See {@link ProductVariant#getImages()},
 * which resolves through this grouping by matching the variant's own color.
 */
@Entity
@Table(name = "product_color_media", uniqueConstraints = @UniqueConstraint(columnNames = {"product_id", "color_id"}))
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class ProductColorMedia extends BaseEntity {

    @EqualsAndHashCode.Exclude
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    // EAGER: tiny reference table read in DTO mapping outside any open transaction.
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "color_id", nullable = false)
    private Color color;

    @EqualsAndHashCode.Exclude
    @OneToMany(mappedBy = "productColorMedia", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("displayOrder ASC")
    private List<ProductImage> images = new ArrayList<>();

    public void addImage(ProductImage image) {
        image.setProductColorMedia(this);
        images.add(image);
    }
}
