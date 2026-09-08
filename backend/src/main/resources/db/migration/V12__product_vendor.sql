-- Adds the supplying vendor to a product (see Product.java / ProductAdminRequest.java).
-- Nullable at the DB/entity level - same as brand_id - even though the admin "Add product"
-- form makes it mandatory; that's enforced in ProductAdminRequest's @NotNull, not here,
-- consistent with how material_id (also NOT NULL app-side, nullable in this ALTER-based
-- style) is handled. Every existing row gets NULL, which is fine since nothing reads this
-- column yet.
ALTER TABLE products ADD COLUMN vendor_id BIGINT;

ALTER TABLE products ADD CONSTRAINT fk_products_vendor FOREIGN KEY (vendor_id) REFERENCES vendors (id);

CREATE INDEX idx_products_vendor ON products (vendor_id);
