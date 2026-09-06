-- The admin product form has always sent/expected a per-image "primary" flag (see
-- frontend/src/types/index.ts AdminProductImage/ProductVariantImageRequest), but
-- product_images never had a column for it and the admin DTOs only carried plain
-- URL strings - so a save that included images failed with a malformed-request-body
-- error (the frontend was posting {url, displayOrder, primary} objects into a field
-- typed List<String>). This adds the missing column; `is_primary` (not `primary`,
-- a MySQL reserved word) backs the entity's `primary` field via @Column(name=...).
ALTER TABLE product_images ADD COLUMN is_primary BOOLEAN NOT NULL DEFAULT FALSE;

-- Backfill: every variant that already has images gets its lowest-display_order
-- image marked primary, so existing products immediately have a well-defined
-- primary image instead of every row defaulting to false. The inner query is
-- wrapped in an extra derived-table SELECT (`AS mins`) because MySQL otherwise
-- rejects a subquery that reads the same table an UPDATE is writing to ("You
-- can't specify target table ... for update in FROM clause") even when it's
-- aliased - materializing it through one more SELECT layer works around that
-- restriction on both MySQL and H2.
UPDATE product_images
SET is_primary = TRUE
WHERE (product_variant_id, display_order) IN (
    SELECT product_variant_id, min_order FROM (
        SELECT product_variant_id, MIN(display_order) AS min_order
        FROM product_images
        GROUP BY product_variant_id
    ) AS mins
);
