package com.clothingretail.cart;

import com.clothingretail.cart.dto.AddCartItemRequest;
import com.clothingretail.cart.dto.CartItemResponse;
import com.clothingretail.cart.dto.CartResponse;
import com.clothingretail.cart.dto.UpdateCartItemRequest;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cart is pure working state (never touches {@code reservedQuantity}/{@code stockQuantity} -
 * that only happens at order creation, see {@code OrderService}). Every read recomputes each
 * item's price/discount/lineTotal and availability live from the current {@link ProductVariant}
 * row rather than storing them, so the cart always reflects up-to-the-second pricing and stock.
 */
@Service
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final CustomerProfileRepository customerProfileRepository;
    private final ProductVariantRepository productVariantRepository;

    public CartService(
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
    public CartResponse getCart(Long userId) {
        CustomerProfile profile = resolveProfile(userId);
        return cartRepository.findByCustomerProfileId(profile.getId())
                .map(this::toResponse)
                .orElseGet(CartResponse::empty);
    }

    @Transactional
    public CartResponse addItem(Long userId, AddCartItemRequest request) {
        CustomerProfile profile = resolveProfile(userId);
        ProductVariant variant = productVariantRepository.findById(request.productVariantId())
                .orElseThrow(() -> new NotFoundException("Product variant not found: " + request.productVariantId()));
        requireActive(variant);

        Cart cart = getOrCreateCart(profile);
        Optional<CartItem> existing = cartItemRepository.findByCartIdAndProductVariantId(cart.getId(), variant.getId());
        int mergedQuantity = existing.map(CartItem::getQuantity).orElse(0) + request.quantity();
        requireAvailable(variant, mergedQuantity);

        if (existing.isPresent()) {
            existing.get().setQuantity(mergedQuantity);
            cartItemRepository.save(existing.get());
        } else {
            CartItem item = new CartItem();
            item.setProductVariant(variant);
            item.setQuantity(mergedQuantity);
            cart.addItem(item);
            cartRepository.save(cart);
        }
        return toResponse(cartRepository.findById(cart.getId()).orElseThrow());
    }

    @Transactional
    public CartResponse updateItem(Long userId, Long itemId, UpdateCartItemRequest request) {
        CustomerProfile profile = resolveProfile(userId);
        Cart cart = requireCart(profile.getId());
        CartItem item = cartItemRepository.findByIdAndCartId(itemId, cart.getId())
                .orElseThrow(() -> new NotFoundException("Cart item not found: " + itemId));

        ProductVariant variant = item.getProductVariant();
        requireActive(variant);
        requireAvailable(variant, request.quantity());

        item.setQuantity(request.quantity());
        cartItemRepository.save(item);
        return toResponse(cartRepository.findById(cart.getId()).orElseThrow());
    }

    @Transactional
    public CartResponse removeItem(Long userId, Long itemId) {
        CustomerProfile profile = resolveProfile(userId);
        Cart cart = requireCart(profile.getId());
        CartItem item = cartItemRepository.findByIdAndCartId(itemId, cart.getId())
                .orElseThrow(() -> new NotFoundException("Cart item not found: " + itemId));
        cart.getItems().remove(item);
        cartItemRepository.delete(item);
        return toResponse(cartRepository.findById(cart.getId()).orElseThrow());
    }

    @Transactional
    public CartResponse clear(Long userId) {
        CustomerProfile profile = resolveProfile(userId);
        Optional<Cart> cartOpt = cartRepository.findByCustomerProfileId(profile.getId());
        if (cartOpt.isEmpty()) {
            return CartResponse.empty();
        }
        Cart cart = cartOpt.get();
        cart.getItems().clear();
        cartRepository.save(cart);
        return toResponse(cartRepository.findById(cart.getId()).orElseThrow());
    }

    private Cart getOrCreateCart(CustomerProfile profile) {
        return cartRepository.findByCustomerProfileId(profile.getId())
                .orElseGet(() -> cartRepository.save(new Cart(profile)));
    }

    private Cart requireCart(Long profileId) {
        return cartRepository.findByCustomerProfileId(profileId)
                .orElseThrow(() -> new NotFoundException("Cart is empty"));
    }

    private void requireActive(ProductVariant variant) {
        boolean productActive = variant.getProduct().getStatus() == ProductStatus.ACTIVE;
        if (!variant.isActive() || !productActive) {
            throw new BadRequestException("This product is no longer available: " + variant.getSku());
        }
    }

    private void requireAvailable(ProductVariant variant, int requestedQuantity) {
        if (requestedQuantity > variant.getAvailableQuantity()) {
            throw new BadRequestException(
                    "Only " + variant.getAvailableQuantity() + " unit(s) of " + variant.getSku() + " are available");
        }
    }

    private CustomerProfile resolveProfile(Long userId) {
        return customerProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new NotFoundException("Customer profile not found for user: " + userId));
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
