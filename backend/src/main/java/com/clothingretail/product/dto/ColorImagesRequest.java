package com.clothingretail.product.dto;

import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * One color's shared image/video set within a {@link ProductAdminRequest} - every size variant of
 * this color displays the exact same {@code images}, so this is submitted once per color rather
 * than once per size (see {@link VariantAdminRequest}, which no longer carries its own images).
 */
public record ColorImagesRequest(
        @NotNull(message = "must not be null") Long colorId, List<VariantImageRequest> images) {}
