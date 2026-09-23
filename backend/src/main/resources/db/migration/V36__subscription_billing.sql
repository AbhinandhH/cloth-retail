-- Software-owner (SUPER_ADMIN) subscription billing for this store - see subscription.SubscriptionBilling's
-- own doc comment for the full model. The seed row below is the safety-critical part: due_date is
-- left NULL and paid TRUE, so deploying this migration to an already-live site can NEVER itself
-- lock anyone out - the lockout (see SubscriptionAccessFilter) only ever engages once SUPER_ADMIN
-- explicitly sets a real due date via the admin UI.
CREATE TABLE subscription_billing (
    id BIGINT PRIMARY KEY,
    monthly_amount DECIMAL(10,2),
    due_date DATE,
    paid BOOLEAN NOT NULL DEFAULT TRUE,
    paid_at TIMESTAMP,
    last_payment_reference VARCHAR(100),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);
INSERT INTO subscription_billing (id, monthly_amount, due_date, paid, paid_at, last_payment_reference, created_at, updated_at)
VALUES (1, NULL, NULL, TRUE, NULL, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

CREATE TABLE subscription_payments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    amount DECIMAL(10,2) NOT NULL,
    gateway_order_id VARCHAR(100) NOT NULL UNIQUE,
    gateway_payment_id VARCHAR(100),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);
CREATE INDEX idx_subscription_payments_status ON subscription_payments (status);
