-- Adds a per-theme "ambient style" switch and seeds the classic pre-redesign
-- look as a selectable theme alongside the richer presets from V4.
--
-- rich_ambient = TRUE (default, matches all 5 existing presets): the
-- full-page AmbientBackground wash shows behind the entire storefront, as
-- built in the "premium redesign" work.
-- rich_ambient = FALSE: the storefront skips that page-wide wash entirely
-- and falls back to plain white behind the product list, with only
-- BrandHero's own small local gold glow (already scoped to the hero section)
-- visible - the original, pre-redesign look the admin asked to keep as a
-- standard option.
ALTER TABLE themes ADD COLUMN rich_ambient BOOLEAN NOT NULL DEFAULT TRUE;

INSERT INTO themes (name, primary_color, secondary_color, accent_color, background_color, text_color, display_order, rich_ambient, created_at, updated_at) VALUES
    ('Classic White & Gold', '#000000', '#d4af37', NULL, '#ffffff', '#111111', 6, FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
