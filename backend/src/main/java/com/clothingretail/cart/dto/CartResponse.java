package com.clothingretail.cart.dto;

import java.math.BigDecimal;
import java.util.List;

public record CartResponse(
        Long id,
        List<CartItemResponse> items,
        BigDecimal subtotal,
        BigDecimal discountTotal,
        BigDecimal total,
        int itemCount) {

    public static CartResponse empty() {
        return new CartResponse(null, List.of(), BigDecimal.ZERO.setScale(2), BigDecimal.ZERO.setScale(2), BigDecimal.ZERO.setScale(2), 0);
    }
}
