package com.clothingretail.product.dto;

import com.clothingretail.product.MediaType;

public record VariantImageResponse(Long id, String url, int displayOrder, boolean primary, MediaType mediaType) {}
