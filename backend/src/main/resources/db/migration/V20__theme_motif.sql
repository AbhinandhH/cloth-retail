-- Adds a per-theme "motif" - unlike rich_ambient (which only toggles the page-wide background
-- wash) or the 5 colors, this switches the site's actual visual language: which display
-- typeface headings use (see frontend index.css's --font-display override), whether the gold
-- shimmer sweep on brand text and the hero's ornamental light bands/glow blobs show at all, and
-- the hero call-to-action button's corner radius. See frontend SiteConfigContext.tsx, which reads
-- this into a data-motif attribute on <html> that those CSS rules key off.
--
-- motif = 'SIGNATURE' (default, matches all 6 existing presets): the ornate, gilded, serif
-- look the storefront already has - shimmering brand text, a softly lit hero with diagonal
-- light bands and glow blobs, a fully pill-shaped call-to-action.
-- motif = 'STUDIO': a calmer, editorial look - a clean geometric display face, flat brand text
-- with no shimmer, a quiet hero with no ornamental lighting, and a squared-off call-to-action.
ALTER TABLE themes ADD COLUMN motif VARCHAR(20) NOT NULL DEFAULT 'SIGNATURE';

INSERT INTO themes (name, primary_color, secondary_color, accent_color, background_color, text_color, display_order, rich_ambient, motif, created_at, updated_at) VALUES
    ('Linen & Ink', '#211d18', '#8c8168', NULL, '#f6f3ec', '#211d18', 7, FALSE, 'STUDIO', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
