-- Seed products with variants and placeholder images so the storefront has real content immediately.

INSERT INTO products (category_id, sub_category_id, brand_id, material_id, name, slug, description, active, created_at, updated_at) VALUES (
    (SELECT id FROM categories WHERE slug = 'kurtis'),
    (SELECT id FROM sub_categories WHERE slug = 'straight-kurtis'),
    (SELECT id FROM brands WHERE name = 'Urban Thread'),
    (SELECT id FROM materials WHERE name = 'Cotton'),
    'Floral Straight Kurti', 'floral-straight-kurti', 'A relaxed-fit straight kurti in a soft floral print, perfect for everyday wear.', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO product_variants (product_id, sku, size_id, color_id, selling_price, discount_percent, stock_quantity, active, created_at, updated_at) VALUES (
    (SELECT id FROM products WHERE slug = 'floral-straight-kurti'),
    'FSK-BLU-M',
    (SELECT id FROM sizes WHERE name = 'M'),
    (SELECT id FROM colors WHERE name = 'Blue'),
    899.00, 10.00, 40, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO product_variants (product_id, sku, size_id, color_id, selling_price, discount_percent, stock_quantity, active, created_at, updated_at) VALUES (
    (SELECT id FROM products WHERE slug = 'floral-straight-kurti'),
    'FSK-BLU-L',
    (SELECT id FROM sizes WHERE name = 'L'),
    (SELECT id FROM colors WHERE name = 'Blue'),
    899.00, 10.00, 35, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO product_variants (product_id, sku, size_id, color_id, selling_price, discount_percent, stock_quantity, active, created_at, updated_at) VALUES (
    (SELECT id FROM products WHERE slug = 'floral-straight-kurti'),
    'FSK-RED-M',
    (SELECT id FROM sizes WHERE name = 'M'),
    (SELECT id FROM colors WHERE name = 'Red'),
    899.00, 0.00, 25, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);


INSERT INTO products (category_id, sub_category_id, brand_id, material_id, name, slug, description, active, created_at, updated_at) VALUES (
    (SELECT id FROM categories WHERE slug = 'kurtis'),
    (SELECT id FROM sub_categories WHERE slug = 'anarkali-kurtis'),
    (SELECT id FROM brands WHERE name = 'Zenith Wear'),
    (SELECT id FROM materials WHERE name = 'Rayon'),
    'Anarkali Embroidered Kurti', 'anarkali-embroidered-kurti', 'Flowing anarkali silhouette with delicate embroidery detailing at the yoke.', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO product_variants (product_id, sku, size_id, color_id, selling_price, discount_percent, stock_quantity, active, created_at, updated_at) VALUES (
    (SELECT id FROM products WHERE slug = 'anarkali-embroidered-kurti'),
    'AEK-GRN-S',
    (SELECT id FROM sizes WHERE name = 'S'),
    (SELECT id FROM colors WHERE name = 'Green'),
    1499.00, 15.00, 20, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO product_variants (product_id, sku, size_id, color_id, selling_price, discount_percent, stock_quantity, active, created_at, updated_at) VALUES (
    (SELECT id FROM products WHERE slug = 'anarkali-embroidered-kurti'),
    'AEK-GRN-M',
    (SELECT id FROM sizes WHERE name = 'M'),
    (SELECT id FROM colors WHERE name = 'Green'),
    1499.00, 15.00, 30, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO product_variants (product_id, sku, size_id, color_id, selling_price, discount_percent, stock_quantity, active, created_at, updated_at) VALUES (
    (SELECT id FROM products WHERE slug = 'anarkali-embroidered-kurti'),
    'AEK-BLK-M',
    (SELECT id FROM sizes WHERE name = 'M'),
    (SELECT id FROM colors WHERE name = 'Black'),
    1499.00, 0.00, 18, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);


INSERT INTO products (category_id, sub_category_id, brand_id, material_id, name, slug, description, active, created_at, updated_at) VALUES (
    (SELECT id FROM categories WHERE slug = 'kurtis'),
    (SELECT id FROM sub_categories WHERE slug = 'a-line-kurtis'),
    (SELECT id FROM brands WHERE name = 'Urban Thread'),
    (SELECT id FROM materials WHERE name = 'Cotton'),
    'A-Line Printed Kurti', 'a-line-printed-kurti', 'A flattering A-line cut in an all-over geometric print, easy to dress up or down.', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO product_variants (product_id, sku, size_id, color_id, selling_price, discount_percent, stock_quantity, active, created_at, updated_at) VALUES (
    (SELECT id FROM products WHERE slug = 'a-line-printed-kurti'),
    'ALP-WHT-M',
    (SELECT id FROM sizes WHERE name = 'M'),
    (SELECT id FROM colors WHERE name = 'White'),
    799.00, 5.00, 50, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO product_variants (product_id, sku, size_id, color_id, selling_price, discount_percent, stock_quantity, active, created_at, updated_at) VALUES (
    (SELECT id FROM products WHERE slug = 'a-line-printed-kurti'),
    'ALP-WHT-L',
    (SELECT id FROM sizes WHERE name = 'L'),
    (SELECT id FROM colors WHERE name = 'White'),
    799.00, 5.00, 45, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);


INSERT INTO products (category_id, sub_category_id, brand_id, material_id, name, slug, description, active, created_at, updated_at) VALUES (
    (SELECT id FROM categories WHERE slug = 'shirts'),
    (SELECT id FROM sub_categories WHERE slug = 'casual-shirts'),
    (SELECT id FROM brands WHERE name = 'Zenith Wear'),
    (SELECT id FROM materials WHERE name = 'Cotton'),
    'Men''s Casual Checked Shirt', 'mens-casual-checked-shirt', 'Breathable cotton shirt in a classic check, tailored for a comfortable everyday fit.', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO product_variants (product_id, sku, size_id, color_id, selling_price, discount_percent, stock_quantity, active, created_at, updated_at) VALUES (
    (SELECT id FROM products WHERE slug = 'mens-casual-checked-shirt'),
    'MCS-BLU-M',
    (SELECT id FROM sizes WHERE name = 'M'),
    (SELECT id FROM colors WHERE name = 'Blue'),
    1199.00, 0.00, 60, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO product_variants (product_id, sku, size_id, color_id, selling_price, discount_percent, stock_quantity, active, created_at, updated_at) VALUES (
    (SELECT id FROM products WHERE slug = 'mens-casual-checked-shirt'),
    'MCS-BLU-L',
    (SELECT id FROM sizes WHERE name = 'L'),
    (SELECT id FROM colors WHERE name = 'Blue'),
    1199.00, 0.00, 55, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO product_variants (product_id, sku, size_id, color_id, selling_price, discount_percent, stock_quantity, active, created_at, updated_at) VALUES (
    (SELECT id FROM products WHERE slug = 'mens-casual-checked-shirt'),
    'MCS-GRN-XL',
    (SELECT id FROM sizes WHERE name = 'XL'),
    (SELECT id FROM colors WHERE name = 'Green'),
    1199.00, 10.00, 20, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);


INSERT INTO products (category_id, sub_category_id, brand_id, material_id, name, slug, description, active, created_at, updated_at) VALUES (
    (SELECT id FROM categories WHERE slug = 'shirts'),
    (SELECT id FROM sub_categories WHERE slug = 'formal-shirts'),
    (SELECT id FROM brands WHERE name = 'Urban Thread'),
    (SELECT id FROM materials WHERE name = 'Cotton'),
    'Men''s Formal White Shirt', 'mens-formal-white-shirt', 'Crisp, wrinkle-resistant formal shirt in classic white - a wardrobe essential.', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO product_variants (product_id, sku, size_id, color_id, selling_price, discount_percent, stock_quantity, active, created_at, updated_at) VALUES (
    (SELECT id FROM products WHERE slug = 'mens-formal-white-shirt'),
    'MFW-WHT-M',
    (SELECT id FROM sizes WHERE name = 'M'),
    (SELECT id FROM colors WHERE name = 'White'),
    1599.00, 0.00, 40, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO product_variants (product_id, sku, size_id, color_id, selling_price, discount_percent, stock_quantity, active, created_at, updated_at) VALUES (
    (SELECT id FROM products WHERE slug = 'mens-formal-white-shirt'),
    'MFW-WHT-L',
    (SELECT id FROM sizes WHERE name = 'L'),
    (SELECT id FROM colors WHERE name = 'White'),
    1599.00, 0.00, 38, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO product_variants (product_id, sku, size_id, color_id, selling_price, discount_percent, stock_quantity, active, created_at, updated_at) VALUES (
    (SELECT id FROM products WHERE slug = 'mens-formal-white-shirt'),
    'MFW-WHT-XL',
    (SELECT id FROM sizes WHERE name = 'XL'),
    (SELECT id FROM colors WHERE name = 'White'),
    1599.00, 20.00, 15, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);


INSERT INTO products (category_id, sub_category_id, brand_id, material_id, name, slug, description, active, created_at, updated_at) VALUES (
    (SELECT id FROM categories WHERE slug = 'dresses'),
    (SELECT id FROM sub_categories WHERE slug = 'maxi-dresses'),
    (SELECT id FROM brands WHERE name = 'Zenith Wear'),
    (SELECT id FROM materials WHERE name = 'Rayon'),
    'Floral Maxi Dress', 'floral-maxi-dress', 'Ankle-length maxi dress in a breezy floral print, cinched at the waist for shape.', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO product_variants (product_id, sku, size_id, color_id, selling_price, discount_percent, stock_quantity, active, created_at, updated_at) VALUES (
    (SELECT id FROM products WHERE slug = 'floral-maxi-dress'),
    'FMD-RED-S',
    (SELECT id FROM sizes WHERE name = 'S'),
    (SELECT id FROM colors WHERE name = 'Red'),
    1899.00, 10.00, 22, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO product_variants (product_id, sku, size_id, color_id, selling_price, discount_percent, stock_quantity, active, created_at, updated_at) VALUES (
    (SELECT id FROM products WHERE slug = 'floral-maxi-dress'),
    'FMD-RED-M',
    (SELECT id FROM sizes WHERE name = 'M'),
    (SELECT id FROM colors WHERE name = 'Red'),
    1899.00, 10.00, 28, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);


INSERT INTO products (category_id, sub_category_id, brand_id, material_id, name, slug, description, active, created_at, updated_at) VALUES (
    (SELECT id FROM categories WHERE slug = 'dresses'),
    (SELECT id FROM sub_categories WHERE slug = 'party-dresses'),
    (SELECT id FROM brands WHERE name = 'Urban Thread'),
    (SELECT id FROM materials WHERE name = 'Rayon'),
    'Sequin Party Dress', 'sequin-party-dress', 'Statement party dress finished with all-over sequin embellishment.', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO product_variants (product_id, sku, size_id, color_id, selling_price, discount_percent, stock_quantity, active, created_at, updated_at) VALUES (
    (SELECT id FROM products WHERE slug = 'sequin-party-dress'),
    'SPD-BLK-S',
    (SELECT id FROM sizes WHERE name = 'S'),
    (SELECT id FROM colors WHERE name = 'Black'),
    2499.00, 25.00, 12, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO product_variants (product_id, sku, size_id, color_id, selling_price, discount_percent, stock_quantity, active, created_at, updated_at) VALUES (
    (SELECT id FROM products WHERE slug = 'sequin-party-dress'),
    'SPD-BLK-M',
    (SELECT id FROM sizes WHERE name = 'M'),
    (SELECT id FROM colors WHERE name = 'Black'),
    2499.00, 25.00, 15, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO product_variants (product_id, sku, size_id, color_id, selling_price, discount_percent, stock_quantity, active, created_at, updated_at) VALUES (
    (SELECT id FROM products WHERE slug = 'sequin-party-dress'),
    'SPD-BLK-L',
    (SELECT id FROM sizes WHERE name = 'L'),
    (SELECT id FROM colors WHERE name = 'Black'),
    2499.00, 25.00, 10, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);


INSERT INTO products (category_id, sub_category_id, brand_id, material_id, name, slug, description, active, created_at, updated_at) VALUES (
    (SELECT id FROM categories WHERE slug = 'dresses'),
    (SELECT id FROM sub_categories WHERE slug = 'casual-dresses'),
    (SELECT id FROM brands WHERE name = 'Zenith Wear'),
    (SELECT id FROM materials WHERE name = 'Cotton'),
    'Casual Summer Dress', 'casual-summer-dress', 'Lightweight cotton dress with a relaxed fit, ideal for warm-weather days.', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO product_variants (product_id, sku, size_id, color_id, selling_price, discount_percent, stock_quantity, active, created_at, updated_at) VALUES (
    (SELECT id FROM products WHERE slug = 'casual-summer-dress'),
    'CSD-WHT-M',
    (SELECT id FROM sizes WHERE name = 'M'),
    (SELECT id FROM colors WHERE name = 'White'),
    1299.00, 0.00, 32, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO product_variants (product_id, sku, size_id, color_id, selling_price, discount_percent, stock_quantity, active, created_at, updated_at) VALUES (
    (SELECT id FROM products WHERE slug = 'casual-summer-dress'),
    'CSD-BLU-M',
    (SELECT id FROM sizes WHERE name = 'M'),
    (SELECT id FROM colors WHERE name = 'Blue'),
    1299.00, 0.00, 30, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);


INSERT INTO products (category_id, sub_category_id, brand_id, material_id, name, slug, description, active, created_at, updated_at) VALUES (
    (SELECT id FROM categories WHERE slug = 't-shirts'),
    (SELECT id FROM sub_categories WHERE slug = 'graphic-tees'),
    (SELECT id FROM brands WHERE name = 'Urban Thread'),
    (SELECT id FROM materials WHERE name = 'Cotton'),
    'Graphic Print T-Shirt', 'graphic-print-t-shirt', 'Soft cotton tee with a bold graphic print on the chest.', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO product_variants (product_id, sku, size_id, color_id, selling_price, discount_percent, stock_quantity, active, created_at, updated_at) VALUES (
    (SELECT id FROM products WHERE slug = 'graphic-print-t-shirt'),
    'GPT-BLK-S',
    (SELECT id FROM sizes WHERE name = 'S'),
    (SELECT id FROM colors WHERE name = 'Black'),
    599.00, 0.00, 70, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO product_variants (product_id, sku, size_id, color_id, selling_price, discount_percent, stock_quantity, active, created_at, updated_at) VALUES (
    (SELECT id FROM products WHERE slug = 'graphic-print-t-shirt'),
    'GPT-BLK-M',
    (SELECT id FROM sizes WHERE name = 'M'),
    (SELECT id FROM colors WHERE name = 'Black'),
    599.00, 0.00, 80, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO product_variants (product_id, sku, size_id, color_id, selling_price, discount_percent, stock_quantity, active, created_at, updated_at) VALUES (
    (SELECT id FROM products WHERE slug = 'graphic-print-t-shirt'),
    'GPT-WHT-M',
    (SELECT id FROM sizes WHERE name = 'M'),
    (SELECT id FROM colors WHERE name = 'White'),
    599.00, 15.00, 60, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);


INSERT INTO products (category_id, sub_category_id, brand_id, material_id, name, slug, description, active, created_at, updated_at) VALUES (
    (SELECT id FROM categories WHERE slug = 't-shirts'),
    (SELECT id FROM sub_categories WHERE slug = 'plain-tees'),
    (SELECT id FROM brands WHERE name = 'Zenith Wear'),
    (SELECT id FROM materials WHERE name = 'Cotton'),
    'Classic Plain T-Shirt', 'classic-plain-t-shirt', 'A no-fuss, everyday tee in a soft cotton jersey knit.', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO product_variants (product_id, sku, size_id, color_id, selling_price, discount_percent, stock_quantity, active, created_at, updated_at) VALUES (
    (SELECT id FROM products WHERE slug = 'classic-plain-t-shirt'),
    'CPT-WHT-S',
    (SELECT id FROM sizes WHERE name = 'S'),
    (SELECT id FROM colors WHERE name = 'White'),
    499.00, 0.00, 90, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO product_variants (product_id, sku, size_id, color_id, selling_price, discount_percent, stock_quantity, active, created_at, updated_at) VALUES (
    (SELECT id FROM products WHERE slug = 'classic-plain-t-shirt'),
    'CPT-WHT-M',
    (SELECT id FROM sizes WHERE name = 'M'),
    (SELECT id FROM colors WHERE name = 'White'),
    499.00, 0.00, 95, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO product_variants (product_id, sku, size_id, color_id, selling_price, discount_percent, stock_quantity, active, created_at, updated_at) VALUES (
    (SELECT id FROM products WHERE slug = 'classic-plain-t-shirt'),
    'CPT-BLU-L',
    (SELECT id FROM sizes WHERE name = 'L'),
    (SELECT id FROM colors WHERE name = 'Blue'),
    499.00, 10.00, 40, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);


-- Variant images
INSERT INTO product_images (product_variant_id, url, display_order, created_at, updated_at) VALUES (
    (SELECT id FROM product_variants WHERE sku = 'FSK-BLU-M'),
    'https://picsum.photos/seed/fsk-blu-m-1/600/800', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO product_images (product_variant_id, url, display_order, created_at, updated_at) VALUES (
    (SELECT id FROM product_variants WHERE sku = 'FSK-BLU-L'),
    'https://picsum.photos/seed/fsk-blu-l-1/600/800', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO product_images (product_variant_id, url, display_order, created_at, updated_at) VALUES (
    (SELECT id FROM product_variants WHERE sku = 'FSK-BLU-L'),
    'https://picsum.photos/seed/fsk-blu-l-2/600/800', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO product_images (product_variant_id, url, display_order, created_at, updated_at) VALUES (
    (SELECT id FROM product_variants WHERE sku = 'FSK-RED-M'),
    'https://picsum.photos/seed/fsk-red-m-1/600/800', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO product_images (product_variant_id, url, display_order, created_at, updated_at) VALUES (
    (SELECT id FROM product_variants WHERE sku = 'FSK-RED-M'),
    'https://picsum.photos/seed/fsk-red-m-2/600/800', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO product_images (product_variant_id, url, display_order, created_at, updated_at) VALUES (
    (SELECT id FROM product_variants WHERE sku = 'FSK-RED-M'),
    'https://picsum.photos/seed/fsk-red-m-3/600/800', 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO product_images (product_variant_id, url, display_order, created_at, updated_at) VALUES (
    (SELECT id FROM product_variants WHERE sku = 'AEK-GRN-S'),
    'https://picsum.photos/seed/aek-grn-s-1/600/800', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO product_images (product_variant_id, url, display_order, created_at, updated_at) VALUES (
    (SELECT id FROM product_variants WHERE sku = 'AEK-GRN-M'),
    'https://picsum.photos/seed/aek-grn-m-1/600/800', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO product_images (product_variant_id, url, display_order, created_at, updated_at) VALUES (
    (SELECT id FROM product_variants WHERE sku = 'AEK-GRN-M'),
    'https://picsum.photos/seed/aek-grn-m-2/600/800', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO product_images (product_variant_id, url, display_order, created_at, updated_at) VALUES (
    (SELECT id FROM product_variants WHERE sku = 'AEK-BLK-M'),
    'https://picsum.photos/seed/aek-blk-m-1/600/800', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO product_images (product_variant_id, url, display_order, created_at, updated_at) VALUES (
    (SELECT id FROM product_variants WHERE sku = 'AEK-BLK-M'),
    'https://picsum.photos/seed/aek-blk-m-2/600/800', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO product_images (product_variant_id, url, display_order, created_at, updated_at) VALUES (
    (SELECT id FROM product_variants WHERE sku = 'AEK-BLK-M'),
    'https://picsum.photos/seed/aek-blk-m-3/600/800', 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO product_images (product_variant_id, url, display_order, created_at, updated_at) VALUES (
    (SELECT id FROM product_variants WHERE sku = 'ALP-WHT-M'),
    'https://picsum.photos/seed/alp-wht-m-1/600/800', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO product_images (product_variant_id, url, display_order, created_at, updated_at) VALUES (
    (SELECT id FROM product_variants WHERE sku = 'ALP-WHT-L'),
    'https://picsum.photos/seed/alp-wht-l-1/600/800', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO product_images (product_variant_id, url, display_order, created_at, updated_at) VALUES (
    (SELECT id FROM product_variants WHERE sku = 'ALP-WHT-L'),
    'https://picsum.photos/seed/alp-wht-l-2/600/800', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO product_images (product_variant_id, url, display_order, created_at, updated_at) VALUES (
    (SELECT id FROM product_variants WHERE sku = 'MCS-BLU-M'),
    'https://picsum.photos/seed/mcs-blu-m-1/600/800', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO product_images (product_variant_id, url, display_order, created_at, updated_at) VALUES (
    (SELECT id FROM product_variants WHERE sku = 'MCS-BLU-L'),
    'https://picsum.photos/seed/mcs-blu-l-1/600/800', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO product_images (product_variant_id, url, display_order, created_at, updated_at) VALUES (
    (SELECT id FROM product_variants WHERE sku = 'MCS-BLU-L'),
    'https://picsum.photos/seed/mcs-blu-l-2/600/800', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO product_images (product_variant_id, url, display_order, created_at, updated_at) VALUES (
    (SELECT id FROM product_variants WHERE sku = 'MCS-GRN-XL'),
    'https://picsum.photos/seed/mcs-grn-xl-1/600/800', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO product_images (product_variant_id, url, display_order, created_at, updated_at) VALUES (
    (SELECT id FROM product_variants WHERE sku = 'MCS-GRN-XL'),
    'https://picsum.photos/seed/mcs-grn-xl-2/600/800', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO product_images (product_variant_id, url, display_order, created_at, updated_at) VALUES (
    (SELECT id FROM product_variants WHERE sku = 'MCS-GRN-XL'),
    'https://picsum.photos/seed/mcs-grn-xl-3/600/800', 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO product_images (product_variant_id, url, display_order, created_at, updated_at) VALUES (
    (SELECT id FROM product_variants WHERE sku = 'MFW-WHT-M'),
    'https://picsum.photos/seed/mfw-wht-m-1/600/800', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO product_images (product_variant_id, url, display_order, created_at, updated_at) VALUES (
    (SELECT id FROM product_variants WHERE sku = 'MFW-WHT-L'),
    'https://picsum.photos/seed/mfw-wht-l-1/600/800', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO product_images (product_variant_id, url, display_order, created_at, updated_at) VALUES (
    (SELECT id FROM product_variants WHERE sku = 'MFW-WHT-L'),
    'https://picsum.photos/seed/mfw-wht-l-2/600/800', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO product_images (product_variant_id, url, display_order, created_at, updated_at) VALUES (
    (SELECT id FROM product_variants WHERE sku = 'MFW-WHT-XL'),
    'https://picsum.photos/seed/mfw-wht-xl-1/600/800', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO product_images (product_variant_id, url, display_order, created_at, updated_at) VALUES (
    (SELECT id FROM product_variants WHERE sku = 'MFW-WHT-XL'),
    'https://picsum.photos/seed/mfw-wht-xl-2/600/800', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO product_images (product_variant_id, url, display_order, created_at, updated_at) VALUES (
    (SELECT id FROM product_variants WHERE sku = 'MFW-WHT-XL'),
    'https://picsum.photos/seed/mfw-wht-xl-3/600/800', 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO product_images (product_variant_id, url, display_order, created_at, updated_at) VALUES (
    (SELECT id FROM product_variants WHERE sku = 'FMD-RED-S'),
    'https://picsum.photos/seed/fmd-red-s-1/600/800', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO product_images (product_variant_id, url, display_order, created_at, updated_at) VALUES (
    (SELECT id FROM product_variants WHERE sku = 'FMD-RED-M'),
    'https://picsum.photos/seed/fmd-red-m-1/600/800', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO product_images (product_variant_id, url, display_order, created_at, updated_at) VALUES (
    (SELECT id FROM product_variants WHERE sku = 'FMD-RED-M'),
    'https://picsum.photos/seed/fmd-red-m-2/600/800', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO product_images (product_variant_id, url, display_order, created_at, updated_at) VALUES (
    (SELECT id FROM product_variants WHERE sku = 'SPD-BLK-S'),
    'https://picsum.photos/seed/spd-blk-s-1/600/800', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO product_images (product_variant_id, url, display_order, created_at, updated_at) VALUES (
    (SELECT id FROM product_variants WHERE sku = 'SPD-BLK-M'),
    'https://picsum.photos/seed/spd-blk-m-1/600/800', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO product_images (product_variant_id, url, display_order, created_at, updated_at) VALUES (
    (SELECT id FROM product_variants WHERE sku = 'SPD-BLK-M'),
    'https://picsum.photos/seed/spd-blk-m-2/600/800', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO product_images (product_variant_id, url, display_order, created_at, updated_at) VALUES (
    (SELECT id FROM product_variants WHERE sku = 'SPD-BLK-L'),
    'https://picsum.photos/seed/spd-blk-l-1/600/800', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO product_images (product_variant_id, url, display_order, created_at, updated_at) VALUES (
    (SELECT id FROM product_variants WHERE sku = 'SPD-BLK-L'),
    'https://picsum.photos/seed/spd-blk-l-2/600/800', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO product_images (product_variant_id, url, display_order, created_at, updated_at) VALUES (
    (SELECT id FROM product_variants WHERE sku = 'SPD-BLK-L'),
    'https://picsum.photos/seed/spd-blk-l-3/600/800', 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO product_images (product_variant_id, url, display_order, created_at, updated_at) VALUES (
    (SELECT id FROM product_variants WHERE sku = 'CSD-WHT-M'),
    'https://picsum.photos/seed/csd-wht-m-1/600/800', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO product_images (product_variant_id, url, display_order, created_at, updated_at) VALUES (
    (SELECT id FROM product_variants WHERE sku = 'CSD-BLU-M'),
    'https://picsum.photos/seed/csd-blu-m-1/600/800', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO product_images (product_variant_id, url, display_order, created_at, updated_at) VALUES (
    (SELECT id FROM product_variants WHERE sku = 'CSD-BLU-M'),
    'https://picsum.photos/seed/csd-blu-m-2/600/800', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO product_images (product_variant_id, url, display_order, created_at, updated_at) VALUES (
    (SELECT id FROM product_variants WHERE sku = 'GPT-BLK-S'),
    'https://picsum.photos/seed/gpt-blk-s-1/600/800', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO product_images (product_variant_id, url, display_order, created_at, updated_at) VALUES (
    (SELECT id FROM product_variants WHERE sku = 'GPT-BLK-M'),
    'https://picsum.photos/seed/gpt-blk-m-1/600/800', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO product_images (product_variant_id, url, display_order, created_at, updated_at) VALUES (
    (SELECT id FROM product_variants WHERE sku = 'GPT-BLK-M'),
    'https://picsum.photos/seed/gpt-blk-m-2/600/800', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO product_images (product_variant_id, url, display_order, created_at, updated_at) VALUES (
    (SELECT id FROM product_variants WHERE sku = 'GPT-WHT-M'),
    'https://picsum.photos/seed/gpt-wht-m-1/600/800', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO product_images (product_variant_id, url, display_order, created_at, updated_at) VALUES (
    (SELECT id FROM product_variants WHERE sku = 'GPT-WHT-M'),
    'https://picsum.photos/seed/gpt-wht-m-2/600/800', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO product_images (product_variant_id, url, display_order, created_at, updated_at) VALUES (
    (SELECT id FROM product_variants WHERE sku = 'GPT-WHT-M'),
    'https://picsum.photos/seed/gpt-wht-m-3/600/800', 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO product_images (product_variant_id, url, display_order, created_at, updated_at) VALUES (
    (SELECT id FROM product_variants WHERE sku = 'CPT-WHT-S'),
    'https://picsum.photos/seed/cpt-wht-s-1/600/800', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO product_images (product_variant_id, url, display_order, created_at, updated_at) VALUES (
    (SELECT id FROM product_variants WHERE sku = 'CPT-WHT-M'),
    'https://picsum.photos/seed/cpt-wht-m-1/600/800', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO product_images (product_variant_id, url, display_order, created_at, updated_at) VALUES (
    (SELECT id FROM product_variants WHERE sku = 'CPT-WHT-M'),
    'https://picsum.photos/seed/cpt-wht-m-2/600/800', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO product_images (product_variant_id, url, display_order, created_at, updated_at) VALUES (
    (SELECT id FROM product_variants WHERE sku = 'CPT-BLU-L'),
    'https://picsum.photos/seed/cpt-blu-l-1/600/800', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO product_images (product_variant_id, url, display_order, created_at, updated_at) VALUES (
    (SELECT id FROM product_variants WHERE sku = 'CPT-BLU-L'),
    'https://picsum.photos/seed/cpt-blu-l-2/600/800', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO product_images (product_variant_id, url, display_order, created_at, updated_at) VALUES (
    (SELECT id FROM product_variants WHERE sku = 'CPT-BLU-L'),
    'https://picsum.photos/seed/cpt-blu-l-3/600/800', 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
