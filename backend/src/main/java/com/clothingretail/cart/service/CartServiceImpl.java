package com.clothingretail.cart.service;

import com.clothingretail.cart.Cart;
import com.clothingretail.cart.CartItem;
import com.clothingretail.cart.dto.AddCartItemRequest;
import com.clothingretail.cart.dto.CartItemResponse;
import com.clothingretail.cart.dto.CartResponse;
import com.clothingretail.cart.dto.UpdateCartItemRequest;
import com.clothingretail.cart.repository.CartItemRepository;
import com.clothingretail.cart.repository.CartRepository;
import com.clothingretail.common.BadRequestException;
import com.clothingretail.common.NotFoundException;
import com.clothingretail.customer.CustomerProfile;
import com.clothingretail.customer.CustomerProfileRepository;
import com.clothingretail.product.ProductImage;
import com.clothingretail.product.ProductStatus;
import com.clothingretail.product.ProductVariant;
import com.clothingretail.product.ProductVariantRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Log4j2
public class CartServiceImpl implements CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final CustomerProfileRepository customerProfileRepository;
    private final ProductVariantRepository productVariantRepository;

    public CartServiceImpl(
            CartRepository cartRepository,
            CartItemRepository cartItemRepository,
            CustomerProfileRepository customerProfileRepository,
            ProductVariantRepository productVariantRepository) {
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.customerProfileRepository = customerProfileRepository;
        this.productVariantRepository = productVariantRepository;
    }

    @Transactional(readOnly = true)
    @Override
    public CartResponse getCart(Long userId) {
        log.info("[1100] Fetch cart userId={}", userId);
        CustomerProfile profile = resolveProfile(userId);
        CartResponse response = cartRepository.findByCustomerProfileId(profile.getId())
                .map(this::toResponse)
                .orElseGet(CartResponse::empty);
        log.info("[1101] Cart fetched userId={} itemCount={}", userId, response.itemCount());
        return response;
    }

    @Transactional
    @Override
    public CartResponse addItem(Long userId, AddCartItemRequest request) {
        log.info("[1102] Add cart item userId={} variantId={} quantity={}", userId, request.productVariantId(), request.quantity());
        CustomerProfile profile = resolveProfile(userId);
        ProductVariant variant = productVariantRepository.findById(request.productVariantId())
                .orElseThrow(() -> {
                    log.error("[1103] Add item failed, variant not found variantId={}", request.productVariantId());
                    return new NotFoundException("Product variant not found: " + request.productVariantId());
                });
        requireActive(variant);

        Cart cart = getOrCreateCart(profile);
        Optional<CartItem> existing = cartItemRepository.findByCartIdAndProductVariantId(cart.getId(), variant.getId());
        int mergedQuantity = existing.map(CartItem::getQuantity).orElse(0) + request.quantity();
        log.info("[1104] Merged quantity computed cartId={} variantId={} mergedQuantity={}", cart.getId(), variant.getId(), mergedQuantity);
        requireAvailable(variant, mergedQuantity);

        if (existing.isPresent()) {
            existing.get().setQuantity(mergedQuantity);
            cartItemRepository.save(existing.get());
            log.info("[1105] Existing cart item quantity updated itemId={} newQuantity={}", existing.get().getId(), mergedQuantity);
        } else {
            CartItem item = new CartItem();
            item.setProductVariant(variant);
            item.setQuantity(mergedQuantity);
            cart.addItem(item);
            cartRepository.save(cart);
            log.info("[1106] New cart item created cartId={} variantId={} quantity={}", cart.getId(), variant.getId(), mergedQuantity);
        }
        CartResponse response = toResponse(cartRepository.findById(cart.getId()).orElseThrow());
        log.info("[1107] Add item completed cartId={} userId={}", cart.getId(), userId);
        return response;
    }

    @Transactional
    @Override
    public CartResponse updateItem(Long userId, Long itemId, UpdateCartItemRequest request) {
        log.info("[1108] Update cart item userId={} itemId={} quantity={}", userId, itemId, request.quantity());
        CustomerProfile profile = resolveProfile(userId);
        Cart cart = requireCart(profile.getId());
        CartItem item = cartItemRepository.findByIdAndCartId(itemId, cart.getId())
                .orElseThrow(() -> {
                    log.error("[1109] Update item failed, cart item not found itemId={} cartId={}", itemId, cart.getId());
                    return new NotFoundException("Cart item not found: " + itemId);
                });

        ProductVariant variant = item.getProductVariant();
        requireActive(variant);
        requireAvailable(variant, request.quantity());

        int oldQuantity = item.getQuantity();
        item.setQuantity(request.quantity());
        cartItemRepository.save(item);
        log.info("[1110] Cart item quantity changed itemId={} oldQuantity={} newQuantity={}", itemId, oldQuantity, request.quantity());
        CartResponse response = toResponse(cartRepository.findById(cart.getId()).orElseThrow());
        log.info("[1111] Update item completed cartId={} itemId={}", cart.getId(), itemId);
        return response;
    }

    @Transactional
    @Override
    public CartResponse removeItem(Long userId, Long itemId) {
        log.info("[1112] Remove cart item userId={} itemId={}", userId, itemId);
        CustomerProfile profile = resolveProfile(userId);
        Cart cart = requireCart(profile.getId());
        CartItem item = cartItemRepository.findByIdAndCartId(itemId, cart.getId())
                .orElseThrow(() -> {
                    log.error("[1113] Remove item failed, cart item not found itemId={} cartId={}", itemId, cart.getId());
                    return new NotFoundException("Cart item not found: " + itemId);
                });
        cart.getItems().remove(item);
        cartItemRepository.delete(item);
        log.info("[1114] Cart item removed itemId={} cartId={}", itemId, cart.getId());
        return toResponse(cartRepository.findById(cart.getId()).orElseThrow());
    }

    @Transactional
    @Override
    public CartResponse clear(Long userId) {
        log.info("[1115] Clear cart userId={}", userId);
        CustomerProfile profile = resolveProfile(userId);
        Optional<Cart> cartOpt = cartRepository.findByCustomerProfileId(profile.getId());
        if (cartOpt.isEmpty()) {
            log.info("[1116] Clear cart no-op, no cart for userId={}", userId);
            return CartResponse.empty();
        }
        Cart cart = cartOpt.get();
        int previousItemCount = cart.getItems().size();
        cart.getItems().clear();
        cartRepository.save(cart);
        log.info("[1117] Cart cleared cartId={} previousItemCount={}", cart.getId(), previousItemCount);
        return toResponse(cartRepository.findById(cart.getId()).orElseThrow());
    }

    private Cart getOrCreateCart(CustomerProfile profile) {
        Optional<Cart> existing = cartRepository.findByCustomerProfileId(profile.getId());
        if (existing.isPresent()) {
            log.info("[1119] Existing cart found profileId={} cartId={}", profile.getId(), existing.get().getId());
            return existing.get();
        }
        Cart created = cartRepository.save(new Cart(profile));
        log.info("[1118] New cart created profileId={} cartId={}", profile.getId(), created.getId());
        return created;
    }

    private Cart requireCart(Long profileId) {
        return cartRepository.findByCustomerProfileId(profileId)
                .orElseThrow(() -> {
                    log.error("[1120] No cart found for profileId={}", profileId);
                    return new NotFoundException("Cart is empty");
                });
    }

    private void requireActive(ProductVariant variant) {
        boolean productActive = variant.getProduct().getStatus() == ProductStatus.ACTIVE;
        if (!variant.isActive() || !productActive) {
            log.error("[1121] Variant unavailable sku={} variantActive={} productActive={}", variant.getSku(), variant.isActive(), productActive);
            throw new BadRequestException("This product is no longer available: " + variant.getSku());
        }
    }

    private void requireAvailable(ProductVariant variant, int requestedQuantity) {
        if (requestedQuantity > variant.getAvailableQuantity()) {
            log.error("[1122] Insufficient stock sku={} requested={} available={}", variant.getSku(), requestedQuantity, variant.getAvailableQuantity());
            throw new BadRequestException(
                    "Only " + variant.getAvailableQuantity() + " unit(s) of " + variant.getSku() + " are available");
        }
    }

    private CustomerProfile resolveProfile(Long userId) {
        CustomerProfile profile = customerProfileRepository.findByUserId(userId)
                .orElseThrow(() -> {
                    log.error("[1124] No customer profile for userId={}", userId);
                    return new NotFoundException("Customer profile not found for user: " + userId);
                });
        log.info("[1123] Resolved customer profile userId={} profileId={}", userId, profile.getId());
        return profile;
    }

    private CartResponse toResponse(Cart cart) {
        List<CartItemResponse> items = cart.getItems().stream()
                .sorted((a, b) -> Long.compare(a.getId(), b.getId()))
                .map(this::toItemResponse)
                .toList();

        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal total = BigDecimal.ZERO;
        int itemCount = 0;
        for (CartItemResponse item : items) {
            subtotal = subtotal.add(item.unitPrice().multiply(BigDecimal.valueOf(item.quantity())));
            total = total.add(item.lineTotal());
            itemCount += item.quantity();
        }
        subtotal = subtotal.setScale(2, RoundingMode.HALF_UP);
        total = total.setScale(2, RoundingMode.HALF_UP);
        BigDecimal discountTotal = subtotal.subtract(total).setScale(2, RoundingMode.HALF_UP);

        return new CartResponse(cart.getId(), items, subtotal, discountTotal, total, itemCount);
    }

    private CartItemResponse toItemResponse(CartItem item) {
        ProductVariant variant = item.getProductVariant();
        BigDecimal unitPrice = variant.getSellingPrice();
        BigDecimal discountPercent = variant.getDiscountPercent();
        BigDecimal lineTotal = lineTotal(unitPrice, discountPercent, item.getQuantity());
        String imageUrl = variant.getImages().stream().findFirst().map(ProductImage::getUrl).orElse(null);
        boolean active = variant.isActive() && variant.getProduct().getStatus() == ProductStatus.ACTIVE;

        return new CartItemResponse(
                item.getId(),
                variant.getId(),
                variant.getProduct().getName(),
                variant.getProduct().getSlug(),
                variant.getSku(),
                variant.getColor().getName(),
                variant.getColor().getHexCode(),
                variant.getSize().getName(),
                imageUrl,
                unitPrice,
                discountPercent,
                lineTotal,
                item.getQuantity(),
                variant.getAvailableQuantity(),
                active);
    }

    private BigDecimal lineTotal(BigDecimal unitPrice, BigDecimal discountPercent, int quantity) {
        BigDecimal gross = unitPrice.multiply(BigDecimal.valueOf(quantity));
        BigDecimal discountFraction = discountPercent.divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP);
        BigDecimal net = gross.multiply(BigDecimal.ONE.subtract(discountFraction));
        return net.setScale(2, RoundingMode.HALF_UP);
    }
}
