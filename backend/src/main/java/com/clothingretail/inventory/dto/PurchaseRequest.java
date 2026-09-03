package com.clothingretail.inventory.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record PurchaseRequest(
        @NotNull(message = "must not be null") Long vendorId,
        String invoiceNumber,
        @NotEmpty(message = "must contain at least one item") @Valid List<PurchaseItemRequest> items) {}
