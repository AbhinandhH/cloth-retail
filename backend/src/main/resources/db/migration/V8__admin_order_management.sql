-- Admin order management: status history, internal notes, shipment tracking, and refunds.
-- Written to be valid both on MySQL 8/9 and on H2 in MODE=MySQL (used by the test profile),
-- same constraint as V1-V7. OrderStatus gains PROCESSING/PACKED/SHIPPED/DELIVERED/RETURNED/
-- REFUNDED and drops the never-used COMPLETED - the orders.status column itself is untouched
-- (still VARCHAR(20), same as V7; every new constant name fits).

-- ── order_items: image snapshot ─────────────────────────────────────────────
ALTER TABLE order_items ADD COLUMN image_url VARCHAR(500);

-- ── payments: customer-selected method ──────────────────────────────────────
ALTER TABLE payments ADD COLUMN payment_method VARCHAR(30);

-- ── order_status_history ─────────────────────────────────────────────────────
-- Append-only, same pattern as inventory_transactions: previous_status is NULL only for the
-- very first row (order creation); changed_by is NULL for a system-driven transition (order
-- creation, payment initiation/webhook outcome, reservation expiry) - the admin UI shows that
-- as "System".
CREATE TABLE order_status_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL,
    previous_status VARCHAR(20),
    new_status VARCHAR(20) NOT NULL,
    changed_by BIGINT,
    reason TEXT,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_order_status_history_order FOREIGN KEY (order_id) REFERENCES orders (id) ON DELETE CASCADE,
    CONSTRAINT fk_order_status_history_changed_by FOREIGN KEY (changed_by) REFERENCES users (id)
);
CREATE INDEX idx_order_status_history_order ON order_status_history (order_id);

-- ── order_notes ───────────────────────────────────────────────────────────────
-- Always admin-authored - created_by is NOT NULL (the endpoint that writes these is admin-only).
CREATE TABLE order_notes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL,
    note TEXT NOT NULL,
    created_by BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_order_notes_order FOREIGN KEY (order_id) REFERENCES orders (id) ON DELETE CASCADE,
    CONSTRAINT fk_order_notes_created_by FOREIGN KEY (created_by) REFERENCES users (id)
);
CREATE INDEX idx_order_notes_order ON order_notes (order_id);

-- ── shipments ─────────────────────────────────────────────────────────────────
-- 1:1 with orders - doesn't exist until an admin first records shipping details via the upsert
-- endpoint (PUT /api/admin/orders/{id}/shipment).
CREATE TABLE shipments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL,
    provider VARCHAR(100),
    tracking_number VARCHAR(100),
    shipment_date DATE,
    delivery_date DATE,
    notes TEXT,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT uq_shipments_order UNIQUE (order_id),
    CONSTRAINT fk_shipments_order FOREIGN KEY (order_id) REFERENCES orders (id) ON DELETE CASCADE
);

-- ── refunds ───────────────────────────────────────────────────────────────────
-- A pure payment-side reversal (see Refund entity javadoc) - does not itself touch inventory.
-- Only ever created against a payment whose status is SUCCESS.
CREATE TABLE refunds (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    payment_id BIGINT NOT NULL,
    amount DECIMAL(10, 2) NOT NULL,
    status VARCHAR(20) NOT NULL,
    reference VARCHAR(100),
    initiated_by BIGINT,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_refunds_payment FOREIGN KEY (payment_id) REFERENCES payments (id),
    CONSTRAINT fk_refunds_initiated_by FOREIGN KEY (initiated_by) REFERENCES users (id)
);
CREATE INDEX idx_refunds_payment ON refunds (payment_id);
