package com.clothingretail.product.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * One image within a {@link VariantAdminRequest}. {@code displayOrder} is accepted for
 * completeness but ProductService always derives the persisted order from this list's own
 * position - client-sent values are never trusted for ordering, only {@code primary} is.
 */
public record VariantImageRequest(@NotBlank(message = "must not be blank") String url, Integer displayOrder, Boolean primary) {}
