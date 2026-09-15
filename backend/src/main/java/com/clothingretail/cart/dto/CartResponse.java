package com.clothingretail.cart.dto;

import java.math.BigDecimal;
import java.util.List;

public record CartResponse(
        Long id,
        List<CartItemResponse> items,
        BigDecimal subtotal,
        BigDecimal discountTotal,
        BigDecimal total,
        // GST already included within `total` (tax-inclusive pricing - see
        // OrderCreationService's own doc comment on the same extraction) - shown here so the
        // customer sees the tax breakdown before placing the order, not just after.
        BigDecimal cgstPercent,
        BigDecimal cgstAmount,
        BigDecimal sgstPercent,
        BigDecimal sgstAmount,
        int itemCount) {

    public static CartResponse empty() {
        BigDecimal zero = BigDecimal.ZERO.setScale(2);
        return new CartResponse(null, List.of(), zero, zero, zero, zero, zero, zero, zero, 0);
    }
}
