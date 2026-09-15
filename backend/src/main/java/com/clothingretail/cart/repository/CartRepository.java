package com.clothingretail.cart.repository;

import com.clothingretail.cart.Cart;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CartRepository extends JpaRepository<Cart, Long> {
    Optional<Cart> findByCustomerProfileId(Long customerProfileId);
}
