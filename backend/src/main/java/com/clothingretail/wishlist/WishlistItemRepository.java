package com.clothingretail.wishlist;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WishlistItemRepository extends JpaRepository<WishlistItem, Long> {

    List<WishlistItem> findByCustomerProfileId(Long customerProfileId);

    Optional<WishlistItem> findByCustomerProfileIdAndProductId(Long customerProfileId, Long productId);

    boolean existsByCustomerProfileIdAndProductId(Long customerProfileId, Long productId);

    /** Just the ids - all a ProductCard grid needs to know which hearts to fill in. */
    @Query("SELECT w.product.id FROM WishlistItem w WHERE w.customerProfile.id = :customerProfileId")
    List<Long> findProductIdsByCustomerProfileId(@Param("customerProfileId") Long customerProfileId);

    /** Same ids, most-recently-wishlisted first - for the wishlist page itself (order matters
     * there; the heart-icon lookup above doesn't care). */
    @Query("SELECT w.product.id FROM WishlistItem w WHERE w.customerProfile.id = :customerProfileId ORDER BY w.createdAt DESC")
    List<Long> findProductIdsByCustomerProfileIdOrderByCreatedAtDesc(@Param("customerProfileId") Long customerProfileId);
}
