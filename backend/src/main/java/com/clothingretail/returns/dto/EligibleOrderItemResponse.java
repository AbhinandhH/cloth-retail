package com.clothingretail.returns.dto;

import java.util.List;

public record EligibleOrderItemResponse(
        Long orderItemId,
        String productName,
        String sku,
        String colorName,
        String sizeName,
        int quantity,
        String imageUrl,
        boolean alreadyHasActiveRequest,
        /** Set only when alreadyHasActiveRequest is true - lets the frontend show that request's live status instead of a dead end. */
        Long activeRequestId,
        List<ReplacementSizeOption> replacementSizes) {}
