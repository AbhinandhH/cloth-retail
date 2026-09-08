-- Per-customer product wishlist (the heart icon on product cards). One row per
-- (customer, product) pair - the unique constraint is the actual duplicate-prevention
-- mechanism (WishlistService's own existence check is just the fast path; this is
-- what holds under concurrent double-clicks/retries). No `active`/`status` snapshot
-- columns here: whether a wishlisted product is still purchasable is derived live
-- from products.status when the wishlist is read, not cached at insert time.
CREATE TABLE wishlist_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    customer_profile_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_wishlist_items_customer_profile FOREIGN KEY (customer_profile_id) REFERENCES customer_profiles (id) ON DELETE CASCADE,
    CONSTRAINT fk_wishlist_items_product FOREIGN KEY (product_id) REFERENCES products (id) ON DELETE CASCADE,
    CONSTRAINT uq_wishlist_items_customer_product UNIQUE (customer_profile_id, product_id)
);
CREATE INDEX idx_wishlist_items_customer_profile ON wishlist_items (customer_profile_id);
