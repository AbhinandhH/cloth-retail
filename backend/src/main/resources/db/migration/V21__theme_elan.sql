-- Seeds "Élan": a premium, editorial fashion-boutique theme. Like Linen & Ink (V20) it uses the
-- 'STUDIO'-family quiet visual language (no shimmer, no hero light-band ornamentation - see
-- frontend index.css's grouped [data-motif="studio"], [data-motif="elan"] rule), but with its own
-- motif value so it gets its own display typeface (a warm editorial serif, not Studio's geometric
-- sans) and its own refined-not-square corner radius - see index.css's [data-motif="elan"] block.
--
-- Unlike every theme before it, this one actually sets accent_color: a muted champagne/bronze,
-- previously a stored-but-unused column (see ThemeService/SiteConfigContext - --brand-accent was
-- future-proofing with nothing consuming it yet).
INSERT INTO themes (name, primary_color, secondary_color, accent_color, background_color, text_color, display_order, rich_ambient, motif, created_at, updated_at) VALUES
    ('Élan', '#2b2420', '#a8998a', '#c2a878', '#f7f3ec', '#2b2420', 8, FALSE, 'ELAN', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
