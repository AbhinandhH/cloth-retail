package com.clothingretail.product.dto;

import com.clothingretail.product.MediaType;
import jakarta.validation.constraints.NotBlank;

/**
 * One image or video within a {@link VariantAdminRequest}. {@code displayOrder} is accepted for
 * completeness but ProductService always derives the persisted order from this list's own
 * position - client-sent values are never trusted for ordering, only {@code primary} is.
 * {@code mediaType} is optional and defaults to {@link MediaType#IMAGE} when omitted, so existing
 * clients that only ever sent images keep working unchanged.
 */
public record VariantImageRequest(
        @NotBlank(message = "must not be blank") String url, Integer displayOrder, Boolean primary, MediaType mediaType) {}
