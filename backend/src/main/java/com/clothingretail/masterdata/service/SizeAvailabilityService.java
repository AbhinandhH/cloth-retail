package com.clothingretail.masterdata.service;

import com.clothingretail.masterdata.dto.AvailableSizeResponse;
import java.util.List;

/**
 * Computes which sizes are pickable for a category: the deduped, order-respecting union of
 * sizes across every active SizeGroup associated with the category, falling back to
 * the full active global Size list when the category has no size groups at all - this
 * fallback is what keeps the pre-existing seeded demo products working once size groups ship,
 * since none of their categories have one configured.
 */
public interface SizeAvailabilityService {

    List<AvailableSizeResponse> availableSizesForCategory(Long categoryId);
}
