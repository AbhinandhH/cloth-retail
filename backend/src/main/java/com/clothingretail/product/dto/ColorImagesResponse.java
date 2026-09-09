package com.clothingretail.product.dto;

import java.util.List;

/** One color's shared image/video set within a {@link ProductAdminResponse} - see {@link ColorImagesRequest}. */
public record ColorImagesResponse(Long colorId, String colorName, List<VariantImageResponse> images) {}
