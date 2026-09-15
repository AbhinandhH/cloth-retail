package com.clothingretail.wishlist.service;

import com.clothingretail.product.dto.ProductSummaryResponse;
import com.clothingretail.wishlist.dto.WishlistResponse;
import java.util.List;

/**
 * Wishlist is scoped to the caller's own CustomerProfile throughout - every operation resolves it
 * fresh from the authenticated userId, never trusts a client-supplied profile/customer id (see
 * CustomerWishlistController, which only ever passes a productId from the request path/body).
 */
public interface WishlistService {

    WishlistResponse list(Long userId);

    /** The wishlist PAGE's data (full product cards), distinct from {@link #list} (just ids, for
     * the heart-icon toggles on every product grid) - most-recently-wishlisted first. */
    List<ProductSummaryResponse> listProducts(Long userId);

    /** Idempotent by design - see WishlistServiceImpl's own doc comment. */
    WishlistResponse add(Long userId, Long productId);

    /** Idempotent the other direction too: removing something already absent is a no-op, not a 404. */
    WishlistResponse remove(Long userId, Long productId);
}
