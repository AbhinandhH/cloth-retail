-- EMPLOYEE already exists in the RoleName enum but was never seeded (V2 only inserted
-- SUPER_ADMIN/ADMIN/CUSTOMER) - add the missing row so it can actually be assigned.
INSERT INTO roles (name, created_at, updated_at)
SELECT 'EMPLOYEE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE name = 'EMPLOYEE');

CREATE TABLE module_permissions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    module VARCHAR(20) NOT NULL,
    can_view BOOLEAN NOT NULL DEFAULT FALSE,
    can_edit BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT uq_module_permissions_user_module UNIQUE (user_id, module),
    CONSTRAINT fk_module_permissions_user FOREIGN KEY (user_id) REFERENCES users (id)
);
CREATE INDEX idx_module_permissions_user ON module_permissions (user_id);

-- Backfill: every existing ADMIN/SUPER_ADMIN user keeps full view+edit access on every operational
-- module they already had unconditional access to before this migration - this deploy must not
-- lock out anyone currently using the app. Cross join against a literal module list since MySQL
-- has no enum-values() equivalent to select from.
INSERT INTO module_permissions (user_id, module, can_view, can_edit, created_at, updated_at)
SELECT u.id, m.module, TRUE, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM users u
JOIN user_roles ur ON ur.user_id = u.id
JOIN roles r ON r.id = ur.role_id
CROSS JOIN (
    SELECT 'DASHBOARD' AS module UNION ALL SELECT 'PRODUCTS' UNION ALL SELECT 'INVENTORY'
    UNION ALL SELECT 'ORDERS' UNION ALL SELECT 'RETURNS' UNION ALL SELECT 'CUSTOMERS'
    UNION ALL SELECT 'MASTERS' UNION ALL SELECT 'REPORTS'
) m
WHERE r.name IN ('ADMIN', 'SUPER_ADMIN');
