-- Admin-configurable GST rates (CGST + SGST), applied to every new order's post-discount goods
-- total (not shipping) at order-creation time - see OrderCreationService and TaxSettings.java.
-- Singleton row, same convention as notification_settings/smtp_settings/site_configuration.
-- Default is 0/0 (no change in behavior for existing installs) until an admin sets real rates in
-- the Tax module.
CREATE TABLE tax_settings (
    id BIGINT PRIMARY KEY,
    cgst_percent DECIMAL(5,2) NOT NULL DEFAULT 0,
    sgst_percent DECIMAL(5,2) NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

INSERT INTO tax_settings (id, cgst_percent, sgst_percent, created_at, updated_at)
VALUES (1, 0.00, 0.00, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Snapshotted onto each order at creation time (both the rate actually applied and the amount it
-- produced), same reasoning as the shipping-address snapshot on this table: a later change to
-- tax_settings must never retroactively change what an already-placed order shows as its bill.
-- Default 0 keeps every pre-existing order's total_amount unchanged.
ALTER TABLE orders ADD COLUMN cgst_percent DECIMAL(5,2) NOT NULL DEFAULT 0;
ALTER TABLE orders ADD COLUMN cgst_amount DECIMAL(10,2) NOT NULL DEFAULT 0;
ALTER TABLE orders ADD COLUMN sgst_percent DECIMAL(5,2) NOT NULL DEFAULT 0;
ALTER TABLE orders ADD COLUMN sgst_amount DECIMAL(10,2) NOT NULL DEFAULT 0;
