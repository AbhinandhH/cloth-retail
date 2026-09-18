ALTER TABLE site_configuration ADD COLUMN reserve_stock_only_at_payment BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE orders ADD COLUMN stock_reserved BOOLEAN NOT NULL DEFAULT TRUE;
