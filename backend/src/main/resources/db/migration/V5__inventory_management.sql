-- Inventory management: product status, optional brand, variant cost/reserved/damaged
-- stock tracking with optimistic locking, a richer inventory_transactions audit trail,
-- and a new damage_records table. Written to be valid both on MySQL 8/9 and on H2 in
-- MODE=MySQL (used by the test profile), same constraint as V1-V4.

-- ── products ────────────────────────────────────────────────────────────────
-- Replace the boolean `active` flag with a richer status enum. Adding the column
-- with a NOT NULL DEFAULT backfills every existing row to 'ACTIVE' in one step
-- (works on both MySQL and H2), then we flip the ones that were inactive.
ALTER TABLE products ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE';
UPDATE products SET status = 'INACTIVE' WHERE active = FALSE;
ALTER TABLE products DROP COLUMN active;
CREATE INDEX idx_products_status ON products (status);

-- Brand becomes optional.
ALTER TABLE products MODIFY COLUMN brand_id BIGINT NULL;

-- Pure UI-prefill template fields for the admin "add variant" form - not resolved
-- or inherited anywhere in application code.
ALTER TABLE products ADD COLUMN base_sku VARCHAR(64);
ALTER TABLE products ADD COLUMN base_selling_price DECIMAL(10, 2);
ALTER TABLE products ADD COLUMN base_cost_price DECIMAL(10, 2);

-- ── product_variants ───────────────────────────────────────────────────────
ALTER TABLE product_variants ADD COLUMN cost_price DECIMAL(10, 2);
ALTER TABLE product_variants ADD COLUMN reserved_quantity INT NOT NULL DEFAULT 0;
ALTER TABLE product_variants ADD COLUMN damaged_quantity INT NOT NULL DEFAULT 0;
-- NULL means "use the system default of 5" wherever it's evaluated.
ALTER TABLE product_variants ADD COLUMN low_stock_threshold INT NULL;
-- JPA optimistic locking (@Version).
ALTER TABLE product_variants ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

ALTER TABLE product_variants ADD CONSTRAINT chk_product_variants_stock_quantity CHECK (stock_quantity >= 0);
ALTER TABLE product_variants ADD CONSTRAINT chk_product_variants_reserved_quantity CHECK (reserved_quantity >= 0);
ALTER TABLE product_variants ADD CONSTRAINT chk_product_variants_damaged_quantity CHECK (damaged_quantity >= 0);

-- idx_product_variants_product already exists (see V1__init_schema.sql).

-- ── inventory_transactions ─────────────────────────────────────────────────
ALTER TABLE inventory_transactions ADD COLUMN reason TEXT;
ALTER TABLE inventory_transactions ADD COLUMN performed_by BIGINT;
ALTER TABLE inventory_transactions ADD COLUMN previous_quantity INT NOT NULL DEFAULT 0;
ALTER TABLE inventory_transactions ADD COLUMN new_quantity INT NOT NULL DEFAULT 0;
ALTER TABLE inventory_transactions ADD CONSTRAINT fk_inventory_transactions_performed_by FOREIGN KEY (performed_by) REFERENCES users (id);
CREATE INDEX idx_inventory_transactions_created_at ON inventory_transactions (created_at);

-- idx_inventory_transactions_variant already exists (see V1__init_schema.sql).

-- ── damage_records ─────────────────────────────────────────────────────────
CREATE TABLE damage_records (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_variant_id BIGINT NOT NULL,
    quantity INT NOT NULL,
    reason VARCHAR(30) NOT NULL,
    notes TEXT,
    reported_by BIGINT,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_damage_records_variant FOREIGN KEY (product_variant_id) REFERENCES product_variants (id),
    CONSTRAINT fk_damage_records_reported_by FOREIGN KEY (reported_by) REFERENCES users (id)
);
CREATE INDEX idx_damage_records_variant ON damage_records (product_variant_id);
