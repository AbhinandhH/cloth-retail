-- Seller GSTIN + registered address for generated order invoices (see order.OrderInvoiceService) -
-- neither existed anywhere before. Admin-only, never exposed on the public /api/configuration
-- response (see SiteConfigurationServiceImpl.toPublicResponse, which is deliberately unchanged).
ALTER TABLE site_configuration ADD COLUMN gstin VARCHAR(20);
ALTER TABLE site_configuration ADD COLUMN registered_address TEXT;
