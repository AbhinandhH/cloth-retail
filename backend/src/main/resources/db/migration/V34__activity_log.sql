-- General-purpose "who changed what, when" trail across the admin console, written automatically
-- for every admin write request (see activity.ActivityLoggingInterceptor) - not a replacement for
-- the domain-specific order_status_history/inventory_transactions tables, which capture semantic
-- detail this table deliberately doesn't (never stores request/response bodies, since some admin
-- write payloads carry passwords - staff creation, SMTP settings).
CREATE TABLE activity_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    actor_id BIGINT,
    http_method VARCHAR(10) NOT NULL,
    path VARCHAR(255) NOT NULL,
    module VARCHAR(50) NOT NULL,
    status_code INT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_activity_log_actor FOREIGN KEY (actor_id) REFERENCES users (id)
);
CREATE INDEX idx_activity_log_actor ON activity_log (actor_id);
CREATE INDEX idx_activity_log_module ON activity_log (module);
CREATE INDEX idx_activity_log_created_at ON activity_log (created_at);
