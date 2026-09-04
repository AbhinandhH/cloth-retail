package com.clothingretail.inventory.dto;

public record StockAdjustResponse(Long variantId, int previousQuantity, int newQuantity, int availableQuantity) {}
