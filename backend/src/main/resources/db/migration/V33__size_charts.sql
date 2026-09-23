-- Reusable, admin-managed measurement tables ("size charts") assignable to products - see
-- masterdata.SizeChart's own doc comment. Modeled the same way size_groups/size_group_sizes
-- is (V6): a parent master table plus an ordered child-row table, cascade-deleted together.
CREATE TABLE size_charts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(150) NOT NULL UNIQUE,
    description TEXT,
    display_order INT NOT NULL DEFAULT 0,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_by BIGINT,
    updated_by BIGINT,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_size_charts_created_by FOREIGN KEY (created_by) REFERENCES users (id),
    CONSTRAINT fk_size_charts_updated_by FOREIGN KEY (updated_by) REFERENCES users (id)
);
CREATE INDEX idx_size_charts_created_by ON size_charts (created_by);
CREATE INDEX idx_size_charts_updated_by ON size_charts (updated_by);

-- Ordered measurement column labels (e.g. "Chest", "Waist", "Foot Length") - a plain JPA
-- @ElementCollection, not a full entity, since a column is nothing more than an ordered label.
CREATE TABLE size_chart_columns (
    size_chart_id BIGINT NOT NULL,
    column_value VARCHAR(100) NOT NULL,
    column_order INT NOT NULL,
    PRIMARY KEY (size_chart_id, column_order),
    CONSTRAINT fk_size_chart_columns_chart FOREIGN KEY (size_chart_id) REFERENCES size_charts (id) ON DELETE CASCADE
);

CREATE TABLE size_chart_rows (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    size_chart_id BIGINT NOT NULL,
    size_label VARCHAR(50) NOT NULL,
    display_order INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_size_chart_rows_chart FOREIGN KEY (size_chart_id) REFERENCES size_charts (id) ON DELETE CASCADE
);
CREATE INDEX idx_size_chart_rows_chart ON size_chart_rows (size_chart_id);

-- Row values, positionally aligned to size_chart_columns by order - the Nth value here
-- corresponds to the Nth column of the row's chart.
CREATE TABLE size_chart_row_values (
    size_chart_row_id BIGINT NOT NULL,
    value_text VARCHAR(255),
    value_order INT NOT NULL,
    PRIMARY KEY (size_chart_row_id, value_order),
    CONSTRAINT fk_size_chart_row_values_row FOREIGN KEY (size_chart_row_id) REFERENCES size_chart_rows (id) ON DELETE CASCADE
);

-- Optional per-product assignment - nullable, no backfill needed (existing products simply
-- have none until an admin sets one), same shape as products.brand_id.
ALTER TABLE products ADD COLUMN size_chart_id BIGINT NULL;
ALTER TABLE products ADD CONSTRAINT fk_products_size_chart FOREIGN KEY (size_chart_id) REFERENCES size_charts (id);
CREATE INDEX idx_products_size_chart ON products (size_chart_id);
