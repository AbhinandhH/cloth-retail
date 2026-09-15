-- Return / Size-Exchange / Damaged-Product-Refund requests. One table covers both request
-- types via request_type (SIZE_EXCHANGE | DAMAGED_PRODUCT) - see ReturnRequest's own doc
-- comment for why this isn't two tables. status/request_type/evidence_status are plain
-- VARCHAR enums (ReturnRequestStatus/RequestType/EvidenceStatus), not master-data tables -
-- same reasoning as orders.status/payments.status: real workflow state that drives code
-- branching, never admin-editable. "reason" is deliberately a free-text column, not a FK to
-- a reasons table - unlike damage_reasons (the inventory module's own physical-stock-write-off
-- reasons), the customer-facing reason here was never asked to be admin-managed.
--
-- Real FK constraints throughout (unlike inventory_transactions.reference_id, which is a soft
-- reference) - these are strict ownership relationships that must never dangle.
CREATE TABLE return_requests (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL,
    order_item_id BIGINT NOT NULL,
    customer_profile_id BIGINT NOT NULL,
    request_type VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    reason TEXT,

    -- SIZE_EXCHANGE only
    requested_variant_id BIGINT,
    requested_size_name VARCHAR(20),

    -- DAMAGED_PRODUCT only
    evidence_status VARCHAR(20) NOT NULL DEFAULT 'NOT_SUBMITTED',
    evidence_reference_code VARCHAR(20),
    evidence_filename VARCHAR(255),
    evidence_content_type VARCHAR(100),
    evidence_uploaded_at TIMESTAMP NULL,

    -- admin review
    admin_note TEXT,
    reviewed_by BIGINT,
    reviewed_at TIMESTAMP NULL,

    -- refund linkage (DAMAGED_PRODUCT only, set exactly once)
    refund_id BIGINT,

    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,

    CONSTRAINT uq_return_requests_evidence_reference_code UNIQUE (evidence_reference_code),
    CONSTRAINT uq_return_requests_refund UNIQUE (refund_id),
    CONSTRAINT fk_return_requests_order FOREIGN KEY (order_id) REFERENCES orders (id),
    CONSTRAINT fk_return_requests_order_item FOREIGN KEY (order_item_id) REFERENCES order_items (id),
    CONSTRAINT fk_return_requests_customer_profile FOREIGN KEY (customer_profile_id) REFERENCES customer_profiles (id),
    CONSTRAINT fk_return_requests_requested_variant FOREIGN KEY (requested_variant_id) REFERENCES product_variants (id) ON DELETE SET NULL,
    CONSTRAINT fk_return_requests_refund FOREIGN KEY (refund_id) REFERENCES refunds (id)
);

CREATE INDEX idx_return_requests_order ON return_requests (order_id);
CREATE INDEX idx_return_requests_customer_profile ON return_requests (customer_profile_id);
CREATE INDEX idx_return_requests_status ON return_requests (status);

-- Duplicate-active-request prevention (no two non-REJECTED requests for the same order item at
-- once) is enforced in ReturnRequestServiceImpl, not a DB constraint - a portable filtered/
-- partial unique index isn't guaranteed identical between MySQL 8 and the H2-MySQL-mode test
-- profile. This index only makes that service-layer existence check fast.
CREATE INDEX idx_return_requests_order_item ON return_requests (order_item_id, status);
