package com.clothingretail.cart.service;

import com.clothingretail.cart.dto.AddCartItemRequest;
import com.clothingretail.cart.dto.CartResponse;
import com.clothingretail.cart.dto.UpdateCartItemRequest;

/**
 * Cart is pure working state (never touches reservedQuantity/stockQuantity - that only happens at
 * order creation). Every read recomputes each item's price/discount/lineTotal and availability
 * live from the current product variant row rather than storing them, so the cart always reflects
 * up-to-the-second pricing and stock.
 */
public interface CartService {

    CartResponse getCart(Long userId);

    CartResponse addItem(Long userId, AddCartItemRequest request);

    CartResponse updateItem(Long userId, Long itemId, UpdateCartItemRequest request);

    CartResponse removeItem(Long userId, Long itemId);

    CartResponse clear(Long userId);
}
