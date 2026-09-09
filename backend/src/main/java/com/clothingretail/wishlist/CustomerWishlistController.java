package com.clothingretail.wishlist;

import com.clothingretail.product.dto.ProductSummaryResponse;
import com.clothingretail.wishlist.dto.WishlistResponse;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Ownership is always derived from the authenticated user's own wishlist - a productId is the only client-supplied id, never a wishlist/customer id. */
@RestController
@RequestMapping("/api/customer/wishlist")
@PreAuthorize("hasRole('CUSTOMER')")
public class CustomerWishlistController {

    private final WishlistService wishlistService;

    public CustomerWishlistController(WishlistService wishlistService) {
        this.wishlistService = wishlistService;
    }

    @GetMapping
    public WishlistResponse list(Authentication authentication) {
        return wishlistService.list(userId(authentication));
    }

    /** The wishlist page's data - full product cards, most-recently-wishlisted first. */
    @GetMapping("/products")
    public List<ProductSummaryResponse> listProducts(Authentication authentication) {
        return wishlistService.listProducts(userId(authentication));
    }

    @PostMapping("/{productId}")
    public WishlistResponse add(Authentication authentication, @PathVariable Long productId) {
        return wishlistService.add(userId(authentication), productId);
    }

    @DeleteMapping("/{productId}")
    public WishlistResponse remove(Authentication authentication, @PathVariable Long productId) {
        return wishlistService.remove(userId(authentication), productId);
    }

    private Long userId(Authentication authentication) {
        return (Long) authentication.getPrincipal();
    }
}
