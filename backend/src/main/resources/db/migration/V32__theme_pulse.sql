-- "Volt & Onyx": a sharp, dark-mode, high-energy theme - electric violet/cyan/magenta on
-- near-black, the "pulse" motif (see index.css's [data-motif="pulse"] rules for its animated
-- aurora hero backdrop, glow-on-hover buttons/cards, and bold geometric display face). Unlike
-- every prior theme, this one is a genuinely different visual language, not just a new palette
-- on the existing ornate/quiet motifs.
INSERT INTO themes (name, primary_color, secondary_color, accent_color, background_color, text_color, display_order, rich_ambient, motif, created_at, updated_at) VALUES
    ('Volt & Onyx', '#6d28d9', '#06b6d4', '#ff2e88', '#0a0a0f', '#f5f5fa', 9, TRUE, 'PULSE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
