package com.clothingretail.wishlist.dto;

import java.util.Set;

/** Every mutation returns the full current set, mirroring CartResponse's "always the whole updated object" convention - the frontend never has to reconcile a partial diff. */
public record WishlistResponse(Set<Long> productIds) {}
