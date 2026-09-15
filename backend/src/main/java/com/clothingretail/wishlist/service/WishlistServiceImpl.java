package com.clothingretail.wishlist.service;

import com.clothingretail.common.NotFoundException;
import com.clothingretail.customer.CustomerProfile;
import com.clothingretail.customer.repository.CustomerProfileRepository;
import com.clothingretail.product.service.ProductService;
import com.clothingretail.product.dto.ProductSummaryResponse;
import com.clothingretail.wishlist.dto.WishlistResponse;
import com.clothingretail.wishlist.repository.WishlistItemRepository;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.extern.log4j.Log4j2;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Wishlist is scoped to the caller's own {@link CustomerProfile} throughout - every
 * operation resolves it fresh from the authenticated userId, never trusts a client-supplied
 * profile/customer id (see {@code CustomerWishlistController}, which only ever passes a
 * productId from the request path/body).
 */
@Service
@Log4j2
public class WishlistServiceImpl implements WishlistService {

    private final WishlistItemRepository wishlistItemRepository;
    private final CustomerProfileRepository customerProfileRepository;
    private final WishlistItemInserter wishlistItemInserter;
    private final ProductService productService;

    public WishlistServiceImpl(
            WishlistItemRepository wishlistItemRepository,
            CustomerProfileRepository customerProfileRepository,
            WishlistItemInserter wishlistItemInserter,
            ProductService productService) {
        this.wishlistItemRepository = wishlistItemRepository;
        this.customerProfileRepository = customerProfileRepository;
        this.wishlistItemInserter = wishlistItemInserter;
        this.productService = productService;
    }

    @Transactional(readOnly = true)
    @Override
    public WishlistResponse list(Long userId) {
        log.info("[1700] List wishlist userId={}", userId);
        CustomerProfile profile = resolveProfile(userId);
        Set<Long> productIds = new HashSet<>(wishlistItemRepository.findProductIdsByCustomerProfileId(profile.getId()));
        log.info("[1701] Wishlist listed userId={} profileId={} count={}", userId, profile.getId(), productIds.size());
        return new WishlistResponse(productIds);
    }

    /** The wishlist PAGE's data (full product cards), distinct from {@link #list} (just ids, for
     * the heart-icon toggles on every product grid) - most-recently-wishlisted first. */
    @Transactional(readOnly = true)
    @Override
    public List<ProductSummaryResponse> listProducts(Long userId) {
        log.info("[1713] List wishlist products userId={}", userId);
        CustomerProfile profile = resolveProfile(userId);
        List<Long> productIds = wishlistItemRepository.findProductIdsByCustomerProfileIdOrderByCreatedAtDesc(profile.getId());
        return productService.listByIds(productIds);
    }

    /**
     * Idempotent by design (not a strict "reject duplicates" add): a double-click/retry
     * racing two adds for the same product should just settle into the one row the unique
     * DB constraint (V10) allows, not surface a confusing 409 (or worse, a raw 500) for what
     * the user experiences as a single tap of a toggle button. existsBy... is the fast path
     * for the common case; the DB constraint is what actually holds under a genuine race -
     * two concurrent adds can both pass the existsBy check before either commits, so the
     * losing insert's {@link DataIntegrityViolationException} is caught below (after
     * {@link WishlistItemInserter}'s own transaction has already rolled back) and turned into
     * "return the current wishlist" instead of an error, mirroring how
     * {@code OrderService.createOrder} handles its own idempotency-key insert race. Not
     * {@code @Transactional} itself, for the same reason {@code OrderService.createOrder}
     * isn't: the actual insert has to happen in a separate bean's transaction
     * ({@link WishlistItemInserter}) so this method can catch the exception only after that
     * transaction has fully rolled back and closed.
     */
    @Override
    public WishlistResponse add(Long userId, Long productId) {
        log.info("[1702] Add to wishlist userId={} productId={}", userId, productId);
        CustomerProfile profile = resolveProfile(userId);
        if (wishlistItemRepository.existsByCustomerProfileIdAndProductId(profile.getId(), productId)) {
            log.info("[1703] Product already wishlisted userId={} profileId={} productId={}", userId, profile.getId(), productId);
            return list(userId);
        }
        try {
            wishlistItemInserter.insert(profile, productId);
            log.info("[1705] Product added to wishlist userId={} profileId={} productId={}", userId, profile.getId(), productId);
        } catch (DataIntegrityViolationException raceLoss) {
            log.error("[1712] Insert race lost for userId={} profileId={} productId={} - returning current wishlist",
                    userId, profile.getId(), productId, raceLoss);
        }
        return list(userId);
    }

    /** Idempotent the other direction too: removing something already absent is a no-op, not a 404 - the end state ("not wishlisted") is what the client asked for either way. */
    @Transactional
    @Override
    public WishlistResponse remove(Long userId, Long productId) {
        log.info("[1706] Remove from wishlist userId={} productId={}", userId, productId);
        CustomerProfile profile = resolveProfile(userId);
        wishlistItemRepository.findByCustomerProfileIdAndProductId(profile.getId(), productId)
                .ifPresentOrElse(
                        item -> {
                            wishlistItemRepository.delete(item);
                            log.info("[1707] Product removed from wishlist userId={} profileId={} productId={}", userId, profile.getId(), productId);
                        },
                        () -> log.info("[1708] Product was not wishlisted, nothing to remove userId={} profileId={} productId={}", userId, profile.getId(), productId));
        return list(userId);
    }

    private CustomerProfile resolveProfile(Long userId) {
        return customerProfileRepository.findByUserId(userId)
                .orElseThrow(() -> {
                    log.error("[1709] No customer profile for userId={}", userId);
                    return new NotFoundException("Customer profile not found for user: " + userId);
                });
    }
}
