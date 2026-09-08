package com.clothingretail.product;

import com.clothingretail.common.BaseEntity;
import com.clothingretail.masterdata.Brand;
import com.clothingretail.masterdata.Category;
import com.clothingretail.masterdata.Material;
import com.clothingretail.masterdata.SubCategory;
import com.clothingretail.masterdata.Vendor;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "products")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class Product extends BaseEntity {

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "sub_category_id")
    private SubCategory subCategory;

    @ManyToOne(fetch = FetchType.EAGER, optional = true)
    @JoinColumn(name = "brand_id", nullable = true)
    private Brand brand;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "material_id", nullable = false)
    private Material material;

    // Nullable at the DB/entity level (see V12 migration - existing rows predate this column
    // and there's no sensible vendor to backfill them with), even though the admin "Add
    // product" form requires it for every NEW product - that's enforced in
    // ProductAdminRequest's @NotNull, the same split brand_id/material_id would need if either
    // one were ever made required after the fact instead of at table-creation time.
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "vendor_id")
    private Vendor vendor;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(nullable = false, unique = true, length = 220)
    private String slug;

    // Plain String, not @Lob/CLOB: the column is TEXT (see V1 migration), and mapping it as
    // CLOB makes Hibernate's schema validator disagree with H2's reported column type.
    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ProductStatus status = ProductStatus.ACTIVE;

    // Pure UI-prefill templates for the admin "add variant" form - never resolved or
    // inherited by any pricing/SKU logic. ProductVariant.sellingPrice/sku remain the
    // sole authoritative source for a variant.
    @Column(name = "base_sku", length = 64)
    private String baseSku;

    @Column(name = "base_selling_price", precision = 10, scale = 2)
    private BigDecimal baseSellingPrice;

    @Column(name = "base_cost_price", precision = 10, scale = 2)
    private BigDecimal baseCostPrice;

    @EqualsAndHashCode.Exclude
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<ProductVariant> variants = new ArrayList<>();

    public void addVariant(ProductVariant variant) {
        variant.setProduct(this);
        variants.add(variant);
    }
}
