package com.clothingretail.wishlist;

import com.clothingretail.common.BaseEntity;
import com.clothingretail.customer.CustomerProfile;
import com.clothingretail.product.Product;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** One (customer, product) pairing - the unique constraint (also enforced at the DB level, see V10) is what actually prevents duplicates under a race; see WishlistService for the fast-path check. */
@Entity
@Table(name = "wishlist_items", uniqueConstraints = @UniqueConstraint(columnNames = {"customer_profile_id", "product_id"}))
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class WishlistItem extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_profile_id", nullable = false)
    private CustomerProfile customerProfile;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    public WishlistItem(CustomerProfile customerProfile, Product product) {
        this.customerProfile = customerProfile;
        this.product = product;
    }
}
