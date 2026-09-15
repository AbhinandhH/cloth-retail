package com.clothingretail.wishlist.service;

import com.clothingretail.common.NotFoundException;
import com.clothingretail.customer.CustomerProfile;
import com.clothingretail.product.Product;
import com.clothingretail.product.repository.ProductRepository;
import com.clothingretail.wishlist.WishlistItem;
import com.clothingretail.wishlist.repository.WishlistItemRepository;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Holds the single {@code @Transactional} method that actually inserts a wishlist row - kept in
 * its own bean (rather than a private method on {@link WishlistService}) specifically so that
 * {@code WishlistService.add}'s try/catch around
 * {@link org.springframework.dao.DataIntegrityViolationException} works correctly: calling a
 * method on {@code this} from within the same class bypasses Spring's transactional proxy
 * entirely (the classic "self-invocation" trap), which would silently turn the inner call into a
 * non-transactional one. Routing through a second bean forces the call through the proxy, so the
 * transaction (and its rollback-on-exception behaviour) is real - the same split
 * OrderService/OrderCreationService already uses for its own insert-race handling.
 */
@Service
@Log4j2
class WishlistItemInserter {

    private final WishlistItemRepository wishlistItemRepository;
    private final ProductRepository productRepository;

    WishlistItemInserter(WishlistItemRepository wishlistItemRepository, ProductRepository productRepository) {
        this.wishlistItemRepository = wishlistItemRepository;
        this.productRepository = productRepository;
    }

    /**
     * save() flushes immediately (IDENTITY generation needs the id right away), so a unique
     * constraint violation from a concurrently-racing add for the same (customer, product) pair
     * surfaces here, inside this transaction, and propagates as a
     * DataIntegrityViolationException to WishlistService.add's catch block instead of leaving a
     * half-broken outer transaction behind.
     */
    @Transactional
    void insert(CustomerProfile profile, Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> {
                    log.error("[1710] Product not found productId={}", productId);
                    return new NotFoundException("Product not found: " + productId);
                });
        wishlistItemRepository.save(new WishlistItem(profile, product));
        log.info("[1711] Wishlist row inserted profileId={} productId={}", profile.getId(), productId);
    }
}
