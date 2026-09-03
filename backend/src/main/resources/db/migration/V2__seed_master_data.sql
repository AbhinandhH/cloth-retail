-- Roles
INSERT INTO roles (name, created_at, updated_at) VALUES
    ('SUPER_ADMIN', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('ADMIN', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('CUSTOMER', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Categories
INSERT INTO categories (name, slug, display_order, active, created_at, updated_at) VALUES
    ('Kurtis', 'kurtis', 1, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('Shirts', 'shirts', 2, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('Dresses', 'dresses', 3, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('T-Shirts', 't-shirts', 4, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('Bottoms', 'bottoms', 5, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Sub-categories (2-3 per category)
INSERT INTO sub_categories (category_id, name, slug, display_order, active, created_at, updated_at) VALUES
    ((SELECT id FROM categories WHERE slug = 'kurtis'), 'Straight Kurtis', 'straight-kurtis', 1, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ((SELECT id FROM categories WHERE slug = 'kurtis'), 'A-Line Kurtis', 'a-line-kurtis', 2, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ((SELECT id FROM categories WHERE slug = 'kurtis'), 'Anarkali Kurtis', 'anarkali-kurtis', 3, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

    ((SELECT id FROM categories WHERE slug = 'shirts'), 'Casual Shirts', 'casual-shirts', 1, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ((SELECT id FROM categories WHERE slug = 'shirts'), 'Formal Shirts', 'formal-shirts', 2, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

    ((SELECT id FROM categories WHERE slug = 'dresses'), 'Maxi Dresses', 'maxi-dresses', 1, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ((SELECT id FROM categories WHERE slug = 'dresses'), 'Party Dresses', 'party-dresses', 2, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ((SELECT id FROM categories WHERE slug = 'dresses'), 'Casual Dresses', 'casual-dresses', 3, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

    ((SELECT id FROM categories WHERE slug = 't-shirts'), 'Graphic Tees', 'graphic-tees', 1, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ((SELECT id FROM categories WHERE slug = 't-shirts'), 'Plain Tees', 'plain-tees', 2, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

    ((SELECT id FROM categories WHERE slug = 'bottoms'), 'Jeans', 'jeans', 1, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ((SELECT id FROM categories WHERE slug = 'bottoms'), 'Trousers', 'trousers', 2, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Sizes
INSERT INTO sizes (name, display_order, active, created_at, updated_at) VALUES
    ('S', 1, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('M', 2, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('L', 3, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('XL', 4, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Colors
INSERT INTO colors (name, hex_code, display_order, active, created_at, updated_at) VALUES
    ('Red', '#D32F2F', 1, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('Blue', '#1976D2', 2, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('Black', '#212121', 3, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('White', '#FAFAFA', 4, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('Green', '#388E3C', 5, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Brands
INSERT INTO brands (name, display_order, active, created_at, updated_at) VALUES
    ('Urban Thread', 1, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('Zenith Wear', 2, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Materials
INSERT INTO materials (name, display_order, active, created_at, updated_at) VALUES
    ('Cotton', 1, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('Rayon', 2, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Vendor
INSERT INTO vendors (name, contact_name, contact_email, contact_phone, active, created_at, updated_at) VALUES
    ('Prime Textiles Pvt Ltd', 'Rakesh Sharma', 'sales@primetextiles.example', '+91-9800000000', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
