package com.clothingretail.order.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record CreateOrderRequest(
        @NotBlank(message = "must not be blank") String idempotencyKey,
        @NotNull(message = "must not be null") Long shippingAddressId,
        String contactPhone,
        /**
         * Which of the caller's own cart lines to check out - null/empty means "the entire
         * cart" (the normal Proceed to Checkout flow). A non-empty list scopes the order to just
         * those lines (the product page's Buy Now flow), leaving every other cart line untouched.
         */
        List<Long> cartItemIds) {}
