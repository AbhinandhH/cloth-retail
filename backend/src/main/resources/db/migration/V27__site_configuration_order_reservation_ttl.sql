-- Admin-configurable order reservation timeout (minutes a placed-but-unpaid order's stock stays
-- reserved before the scheduled cleanup job - OrderReservationCleanupJob - releases it and
-- cancels the order). Previously only settable via the ORDER_RESERVATION_TTL_MINUTES env var
-- (app.order.reservation-ttl-minutes in application.yml, defaulting to 15) - this column lets an
-- admin change it at runtime from the Site Configuration screen instead of needing a redeploy.
-- Lives on site_configuration (not a new table) since it's a single store-wide operational
-- setting, same singleton-row shape as everything else there.
ALTER TABLE site_configuration ADD COLUMN order_reservation_ttl_minutes INT NOT NULL DEFAULT 15;
