-- Cart / Order / Payment system. Written to be valid both on MySQL 8/9 and on H2 in
-- MODE=MySQL (used by the test profile), same constraint as V1-V6.
--
-- Design notes (see OrderService/PaymentService for the full story):
--   * carts/cart_items are pure working state, not historical records - cart_items'
--     product_variant_id FK cascades on delete, same as other non-historical variant
--     references in this schema (e.g. product_images).
--   * orders/order_items ARE historical records - order_items keeps its own
--     product_name/sku/color_name/size_name/... snapshot columns as the source of
--     truth for display, so an order stays fully readable even if its product_variant_id
--     is later nulled out (ON DELETE SET NULL) by a variant being deleted.
--   * orders.status / payments.status are plain VARCHAR enums (OrderStatus/PaymentStatus),
--     not master-data tables - same reasoning as products.status (ProductStatus): real
--     workflow state that drives code branching, never admin-editable.

-- ── carts ────────────────────────────────────────────────────────────────────
CREATE TABLE carts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    customer_profile_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT uq_carts_customer_profile UNIQUE (customer_profile_id),
    CONSTRAINT fk_carts_customer_profile FOREIGN KEY (customer_profile_id) REFERENCES customer_profiles (id) ON DELETE CASCADE
);

CREATE TABLE cart_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    cart_id BIGINT NOT NULL,
    product_variant_id BIGINT NOT NULL,
    quantity INT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT uq_cart_items_cart_variant UNIQUE (cart_id, product_variant_id),
    CONSTRAINT chk_cart_items_quantity CHECK (quantity > 0),
    CONSTRAINT fk_cart_items_cart FOREIGN KEY (cart_id) REFERENCES carts (id) ON DELETE CASCADE,
    CONSTRAINT fk_cart_items_variant FOREIGN KEY (product_variant_id) REFERENCES product_variants (id) ON DELETE CASCADE
);
CREATE INDEX idx_cart_items_cart ON cart_items (cart_id);
CREATE INDEX idx_cart_items_variant ON cart_items (product_variant_id);

-- ── orders ───────────────────────────────────────────────────────────────────
CREATE TABLE orders (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_number VARCHAR(40) NOT NULL,
    customer_profile_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL,
    idempotency_key VARCHAR(100) NOT NULL,
    subtotal DECIMAL(10, 2) NOT NULL,
    discount_total DECIMAL(10, 2) NOT NULL DEFAULT 0,
    shipping_charge DECIMAL(10, 2) NOT NULL DEFAULT 0,
    total_amount DECIMAL(10, 2) NOT NULL,
    shipping_address_line1 VARCHAR(255) NOT NULL,
    shipping_address_line2 VARCHAR(255),
    shipping_city VARCHAR(100) NOT NULL,
    shipping_state VARCHAR(100) NOT NULL,
    shipping_postal_code VARCHAR(20) NOT NULL,
    shipping_country VARCHAR(100) NOT NULL,
    contact_name VARCHAR(150) NOT NULL,
    contact_phone VARCHAR(20) NOT NULL,
    reservation_expires_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT uq_orders_order_number UNIQUE (order_number),
    CONSTRAINT uq_orders_idempotency_key UNIQUE (idempotency_key),
    CONSTRAINT fk_orders_customer_profile FOREIGN KEY (customer_profile_id) REFERENCES customer_profiles (id)
);
CREATE INDEX idx_orders_customer_profile ON orders (customer_profile_id);
CREATE INDEX idx_orders_status ON orders (status);
CREATE INDEX idx_orders_reservation_expires_at ON orders (reservation_expires_at);

CREATE TABLE order_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL,
    product_variant_id BIGINT,
    product_name VARCHAR(200) NOT NULL,
    sku VARCHAR(64) NOT NULL,
    color_name VARCHAR(50) NOT NULL,
    size_name VARCHAR(20) NOT NULL,
    quantity INT NOT NULL,
    unit_price DECIMAL(10, 2) NOT NULL,
    discount_percent DECIMAL(5, 2) NOT NULL DEFAULT 0,
    line_total DECIMAL(10, 2) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_order_items_order FOREIGN KEY (order_id) REFERENCES orders (id) ON DELETE CASCADE,
    CONSTRAINT fk_order_items_variant FOREIGN KEY (product_variant_id) REFERENCES product_variants (id) ON DELETE SET NULL
);
CREATE INDEX idx_order_items_order ON order_items (order_id);
CREATE INDEX idx_order_items_variant ON order_items (product_variant_id);

-- ── payments ─────────────────────────────────────────────────────────────────
CREATE TABLE payments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL,
    gateway_reference VARCHAR(100) NOT NULL,
    webhook_event_id VARCHAR(100),
    amount DECIMAL(10, 2) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT uq_payments_gateway_reference UNIQUE (gateway_reference),
    CONSTRAINT uq_payments_webhook_event_id UNIQUE (webhook_event_id),
    CONSTRAINT fk_payments_order FOREIGN KEY (order_id) REFERENCES orders (id)
);
CREATE INDEX idx_payments_order ON payments (order_id);
CREATE INDEX idx_payments_status ON payments (status);
