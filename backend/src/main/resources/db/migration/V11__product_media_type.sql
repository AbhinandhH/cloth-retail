-- Adds video support to product media (see MediaType.java / ProductImage.java). Every
-- existing row is a plain image, so the column default alone backfills them correctly -
-- no UPDATE needed (unlike V9, which had to compute a value per row).
ALTER TABLE product_images ADD COLUMN media_type VARCHAR(10) NOT NULL DEFAULT 'IMAGE';

ALTER TABLE product_images ADD CONSTRAINT chk_product_images_media_type CHECK (media_type IN ('IMAGE', 'VIDEO'));
