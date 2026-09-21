-- Admin-configurable idle session timeout (minutes of inactivity before a user - customer,
-- admin, or employee - is automatically logged out). Lives on site_configuration, same
-- singleton-row shape/reasoning as order_reservation_ttl_minutes (V27): a single store-wide
-- operational setting, not worth its own table.
ALTER TABLE site_configuration ADD COLUMN idle_timeout_minutes INT NOT NULL DEFAULT 30;

-- Tracks when a refresh token was last used to mint a new access token (reset on every
-- rotation - see AuthServiceImpl#refresh), so the idle window above can be enforced server-side:
-- a refresh presented after idle_timeout_minutes have elapsed since last_used_at is rejected
-- instead of rotated. Backfilled to created_at for existing sessions so nobody currently logged
-- in is treated as already idle-expired the moment this migration runs.
ALTER TABLE refresh_tokens ADD COLUMN last_used_at TIMESTAMP NULL;
UPDATE refresh_tokens SET last_used_at = created_at WHERE last_used_at IS NULL;
ALTER TABLE refresh_tokens MODIFY COLUMN last_used_at TIMESTAMP NOT NULL;
