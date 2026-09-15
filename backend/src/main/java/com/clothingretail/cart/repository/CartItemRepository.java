package com.clothingretail.cart.repository;

import com.clothingretail.cart.CartItem;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {
    Optional<CartItem> findByCartIdAndProductVariantId(Long cartId, Long productVariantId);

    Optional<CartItem> findByIdAndCartId(Long id, Long cartId);
}
