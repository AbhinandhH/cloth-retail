package com.clothingretail.cart;

import com.clothingretail.common.BaseEntity;
import com.clothingretail.customer.CustomerProfile;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** One cart per {@link CustomerProfile}, created lazily on first add - see {@code CartService}. */
@Entity
@Table(name = "carts")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class Cart extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_profile_id", nullable = false, unique = true)
    private CustomerProfile customerProfile;

    @EqualsAndHashCode.Exclude
    @OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CartItem> items = new ArrayList<>();

    public Cart(CustomerProfile customerProfile) {
        this.customerProfile = customerProfile;
    }

    public void addItem(CartItem item) {
        item.setCart(this);
        items.add(item);
    }
}
