-- Images/videos used to belong to a single size+color ProductVariant, so two
-- sizes of the same color could (and, in existing data, always did) end up
-- showing different photos - a garment looks the same regardless of size, it
-- only fits differently. This introduces the missing grouping: one
-- product_color_media row per (product, color), and re-points product_images
-- at that instead of at any individual size variant, so every size of a
-- color now shares the exact same photos by construction.

CREATE TABLE product_color_media (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id BIGINT NOT NULL,
    color_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT uq_product_color_media UNIQUE (product_id, color_id),
    CONSTRAINT fk_product_color_media_product FOREIGN KEY (product_id) REFERENCES products (id) ON DELETE CASCADE,
    CONSTRAINT fk_product_color_media_color FOREIGN KEY (color_id) REFERENCES colors (id)
);
CREATE INDEX idx_product_color_media_product ON product_color_media (product_id);

-- One group per (product, color) combination that already exists among
-- variants, whether or not it currently has any images - keeps the grouping
-- complete for every color the product actually sells.
INSERT INTO product_color_media (product_id, color_id, created_at, updated_at)
SELECT DISTINCT pv.product_id, pv.color_id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM product_variants pv;

-- Re-point every existing image at its variant's (product, color) group
-- instead of the variant itself - this is where different sizes' previously
-- separate image sets get merged into one shared set per color. A plain
-- correlated scalar subquery (not a JOIN) keeps this portable across MySQL
-- and H2 (used in tests).
ALTER TABLE product_images ADD COLUMN product_color_media_id BIGINT NULL;

UPDATE product_images pi
SET pi.product_color_media_id = (
    SELECT pcm.id
    FROM product_variants pv
    JOIN product_color_media pcm ON pcm.product_id = pv.product_id AND pcm.color_id = pv.color_id
    WHERE pv.id = pi.product_variant_id
);

-- Different sizes may have had the exact same URL uploaded independently -
-- collapse those now-duplicate rows within a group rather than showing the
-- same photo twice, keeping the earliest (lowest id). The extra derived-table
-- SELECT (`AS dups`) works around MySQL rejecting a subquery that reads the
-- same table a DELETE is writing to, even when aliased (same restriction V9
-- hit for UPDATE - see its own comment).
DELETE FROM product_images
WHERE id IN (
    SELECT id FROM (
        SELECT pi1.id
        FROM product_images pi1
        JOIN product_images pi2
          ON pi2.product_color_media_id = pi1.product_color_media_id
         AND pi2.url = pi1.url
         AND pi2.id < pi1.id
    ) AS dups
);

-- Different sizes' separate image sets are now merged into one group each, so
-- display_order and is_primary need renumbering per group: existing primary
-- images win the order tie-break, and exactly one row per group ends up
-- primary. Ranking via a correlated COUNT (rows that sort before this one)
-- avoids relying on UPDATE...JOIN syntax, which MySQL and H2 don't both
-- support identically - COUNT-based ranking only needs portable subqueries,
-- same derived-table-wrap workaround as above.
UPDATE product_images pi
SET pi.display_order = (
    SELECT COUNT(*)
    FROM (SELECT id, product_color_media_id, display_order, is_primary FROM product_images) AS pi2
    WHERE pi2.product_color_media_id = pi.product_color_media_id
      AND (
           (pi2.is_primary AND NOT pi.is_primary)
        OR (pi2.is_primary = pi.is_primary AND pi2.display_order < pi.display_order)
        OR (pi2.is_primary = pi.is_primary AND pi2.display_order = pi.display_order AND pi2.id < pi.id)
      )
);

UPDATE product_images SET is_primary = (display_order = 0);

ALTER TABLE product_images MODIFY COLUMN product_color_media_id BIGINT NOT NULL;
ALTER TABLE product_images DROP FOREIGN KEY fk_product_images_variant;
DROP INDEX idx_product_images_variant ON product_images;
ALTER TABLE product_images DROP COLUMN product_variant_id;
ALTER TABLE product_images ADD CONSTRAINT fk_product_images_color_media FOREIGN KEY (product_color_media_id) REFERENCES product_color_media (id) ON DELETE CASCADE;
CREATE INDEX idx_product_images_color_media ON product_images (product_color_media_id);
