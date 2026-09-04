-- Master data management: adds created_by/updated_by audit columns to the existing master
-- tables, converts damage reasons from a fixed enum to a real editable master-data entity
-- (1:1 seed from the old enum so existing references convert cleanly), and introduces
-- SizeGroup (named, ordered, category-scoped subsets of the global sizes list). Written to
-- be valid both on MySQL 8/9 and on H2 in MODE=MySQL (used by the test profile), same
-- constraint as V1-V5.

-- ── audit columns on existing master tables ───────────────────────────────────
-- Same shape as inventory_transactions.performed_by / damage_records.reported_by (V5):
-- a plain nullable BIGINT with an FK to users(id), no ON DELETE action.
ALTER TABLE categories ADD COLUMN created_by BIGINT;
ALTER TABLE categories ADD COLUMN updated_by BIGINT;
ALTER TABLE categories ADD CONSTRAINT fk_categories_created_by FOREIGN KEY (created_by) REFERENCES users (id);
ALTER TABLE categories ADD CONSTRAINT fk_categories_updated_by FOREIGN KEY (updated_by) REFERENCES users (id);
CREATE INDEX idx_categories_created_by ON categories (created_by);
CREATE INDEX idx_categories_updated_by ON categories (updated_by);

ALTER TABLE sub_categories ADD COLUMN created_by BIGINT;
ALTER TABLE sub_categories ADD COLUMN updated_by BIGINT;
ALTER TABLE sub_categories ADD CONSTRAINT fk_sub_categories_created_by FOREIGN KEY (created_by) REFERENCES users (id);
ALTER TABLE sub_categories ADD CONSTRAINT fk_sub_categories_updated_by FOREIGN KEY (updated_by) REFERENCES users (id);
CREATE INDEX idx_sub_categories_created_by ON sub_categories (created_by);
CREATE INDEX idx_sub_categories_updated_by ON sub_categories (updated_by);

ALTER TABLE sizes ADD COLUMN created_by BIGINT;
ALTER TABLE sizes ADD COLUMN updated_by BIGINT;
ALTER TABLE sizes ADD CONSTRAINT fk_sizes_created_by FOREIGN KEY (created_by) REFERENCES users (id);
ALTER TABLE sizes ADD CONSTRAINT fk_sizes_updated_by FOREIGN KEY (updated_by) REFERENCES users (id);
CREATE INDEX idx_sizes_created_by ON sizes (created_by);
CREATE INDEX idx_sizes_updated_by ON sizes (updated_by);

ALTER TABLE colors ADD COLUMN created_by BIGINT;
ALTER TABLE colors ADD COLUMN updated_by BIGINT;
ALTER TABLE colors ADD CONSTRAINT fk_colors_created_by FOREIGN KEY (created_by) REFERENCES users (id);
ALTER TABLE colors ADD CONSTRAINT fk_colors_updated_by FOREIGN KEY (updated_by) REFERENCES users (id);
CREATE INDEX idx_colors_created_by ON colors (created_by);
CREATE INDEX idx_colors_updated_by ON colors (updated_by);

ALTER TABLE brands ADD COLUMN created_by BIGINT;
ALTER TABLE brands ADD COLUMN updated_by BIGINT;
ALTER TABLE brands ADD CONSTRAINT fk_brands_created_by FOREIGN KEY (created_by) REFERENCES users (id);
ALTER TABLE brands ADD CONSTRAINT fk_brands_updated_by FOREIGN KEY (updated_by) REFERENCES users (id);
CREATE INDEX idx_brands_created_by ON brands (created_by);
CREATE INDEX idx_brands_updated_by ON brands (updated_by);

ALTER TABLE materials ADD COLUMN created_by BIGINT;
ALTER TABLE materials ADD COLUMN updated_by BIGINT;
ALTER TABLE materials ADD CONSTRAINT fk_materials_created_by FOREIGN KEY (created_by) REFERENCES users (id);
ALTER TABLE materials ADD CONSTRAINT fk_materials_updated_by FOREIGN KEY (updated_by) REFERENCES users (id);
CREATE INDEX idx_materials_created_by ON materials (created_by);
CREATE INDEX idx_materials_updated_by ON materials (updated_by);

ALTER TABLE vendors ADD COLUMN created_by BIGINT;
ALTER TABLE vendors ADD COLUMN updated_by BIGINT;
ALTER TABLE vendors ADD CONSTRAINT fk_vendors_created_by FOREIGN KEY (created_by) REFERENCES users (id);
ALTER TABLE vendors ADD CONSTRAINT fk_vendors_updated_by FOREIGN KEY (updated_by) REFERENCES users (id);
CREATE INDEX idx_vendors_created_by ON vendors (created_by);
CREATE INDEX idx_vendors_updated_by ON vendors (updated_by);

-- ── damage_reasons ─────────────────────────────────────────────────────────────
CREATE TABLE damage_reasons (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    code VARCHAR(30),
    description TEXT,
    display_order INT NOT NULL DEFAULT 0,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_by BIGINT,
    updated_by BIGINT,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT uq_damage_reasons_code UNIQUE (code),
    CONSTRAINT fk_damage_reasons_created_by FOREIGN KEY (created_by) REFERENCES users (id),
    CONSTRAINT fk_damage_reasons_updated_by FOREIGN KEY (updated_by) REFERENCES users (id)
);
CREATE INDEX idx_damage_reasons_created_by ON damage_reasons (created_by);
CREATE INDEX idx_damage_reasons_updated_by ON damage_reasons (updated_by);

-- 1:1 with the old DamageReason enum constants (code = the old enum name), so the
-- damage_records backfill below can match every existing row unambiguously.
INSERT INTO damage_reasons (name, code, description, display_order, active, created_at, updated_at) VALUES
    ('Defective', 'DEFECTIVE', 'Item was defective on arrival', 1, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('Transit Damage', 'TRANSIT_DAMAGE', 'Damaged in transit from the vendor', 2, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('Warehouse Damage', 'WAREHOUSE_DAMAGE', 'Damaged while stored in the warehouse', 3, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('Return Damage', 'RETURN_DAMAGE', 'Damaged when returned by a customer', 4, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('Other', 'OTHER', NULL, 5, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- ── damage_records: enum column -> FK ───────────────────────────────────────────
ALTER TABLE damage_records ADD COLUMN damage_reason_id BIGINT;
UPDATE damage_records SET damage_reason_id = (SELECT id FROM damage_reasons WHERE code = damage_records.reason) WHERE reason IS NOT NULL;
ALTER TABLE damage_records MODIFY COLUMN damage_reason_id BIGINT NOT NULL;
ALTER TABLE damage_records DROP COLUMN reason;
ALTER TABLE damage_records ADD CONSTRAINT fk_damage_records_damage_reason FOREIGN KEY (damage_reason_id) REFERENCES damage_reasons (id);
CREATE INDEX idx_damage_records_damage_reason ON damage_records (damage_reason_id);

-- ── size_groups ──────────────────────────────────────────────────────────────
CREATE TABLE size_groups (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    description TEXT,
    display_order INT NOT NULL DEFAULT 0,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_by BIGINT,
    updated_by BIGINT,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_size_groups_created_by FOREIGN KEY (created_by) REFERENCES users (id),
    CONSTRAINT fk_size_groups_updated_by FOREIGN KEY (updated_by) REFERENCES users (id)
);
CREATE INDEX idx_size_groups_created_by ON size_groups (created_by);
CREATE INDEX idx_size_groups_updated_by ON size_groups (updated_by);

CREATE TABLE size_group_categories (
    size_group_id BIGINT NOT NULL,
    category_id BIGINT NOT NULL,
    PRIMARY KEY (size_group_id, category_id),
    CONSTRAINT fk_size_group_categories_size_group FOREIGN KEY (size_group_id) REFERENCES size_groups (id) ON DELETE CASCADE,
    CONSTRAINT fk_size_group_categories_category FOREIGN KEY (category_id) REFERENCES categories (id) ON DELETE CASCADE
);
CREATE INDEX idx_size_group_categories_category ON size_group_categories (category_id);

CREATE TABLE size_group_sizes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    size_group_id BIGINT NOT NULL,
    size_id BIGINT NOT NULL,
    display_order INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_size_group_sizes_size_group FOREIGN KEY (size_group_id) REFERENCES size_groups (id) ON DELETE CASCADE,
    CONSTRAINT fk_size_group_sizes_size FOREIGN KEY (size_id) REFERENCES sizes (id)
);
CREATE INDEX idx_size_group_sizes_size_group ON size_group_sizes (size_group_id);
CREATE INDEX idx_size_group_sizes_size ON size_group_sizes (size_id);
