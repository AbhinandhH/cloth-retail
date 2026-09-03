-- Site configuration module: selectable storefront themes + a singleton
-- site_configuration row (branding, contact info, login-page visuals).

CREATE TABLE themes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    primary_color VARCHAR(7) NOT NULL,
    secondary_color VARCHAR(7) NOT NULL,
    accent_color VARCHAR(7),
    background_color VARCHAR(7) NOT NULL,
    text_color VARCHAR(7) NOT NULL,
    display_order INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT uq_themes_name UNIQUE (name)
);

CREATE TABLE site_configuration (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    active_theme_id BIGINT NOT NULL,
    business_name VARCHAR(150) NOT NULL,
    tagline VARCHAR(255),
    logo_url VARCHAR(500),
    favicon_url VARCHAR(500),
    contact_email VARCHAR(255),
    contact_phone VARCHAR(30),
    instagram_url VARCHAR(500),
    whatsapp_number VARCHAR(30),
    facebook_url VARCHAR(500),
    footer_text VARCHAR(500),
    login_background_image_url VARCHAR(500),
    login_promo_image_url VARCHAR(500),
    login_promo_text VARCHAR(500),
    registration_image_url VARCHAR(500),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_site_configuration_active_theme FOREIGN KEY (active_theme_id) REFERENCES themes (id)
);

-- Seed the 5 selectable themes. Inserted in display order into a fresh table,
-- so auto-increment assigns ids 1-5 matching display_order 1-5.
INSERT INTO themes (name, primary_color, secondary_color, accent_color, background_color, text_color, display_order, created_at, updated_at) VALUES
    ('Rose & Charcoal', '#e11d48', '#18181b', NULL, '#fafafa', '#18181b', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('Black & Gold', '#000000', '#d4af37', NULL, '#ffffff', '#111111', 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('Dark Green & Cream', '#1b4332', '#f5f0e6', NULL, '#ffffff', '#1b4332', 3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('Maroon & Gold', '#5c0011', '#d4af37', NULL, '#ffffff', '#2d0000', 4, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('Black & Rose Gold', '#000000', '#b76e79', NULL, '#0a0a0a', '#f5f5f5', 5, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Singleton configuration row, id fixed at 1. Rose & Charcoal (the theme that
-- matches the storefront's current hardcoded look) is the active theme, so
-- nothing visually changes until an admin picks another one.
INSERT INTO site_configuration (id, active_theme_id, business_name, tagline, footer_text, created_at, updated_at) VALUES
    (1, (SELECT id FROM themes WHERE name = 'Rose & Charcoal'), 'ThreadCo', 'Everyday fashion, delivered to your door', '© 2026 ThreadCo. All rights reserved.', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
