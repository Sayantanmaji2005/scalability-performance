-- Add missing columns to products table
ALTER TABLE products ADD COLUMN IF NOT EXISTS description VARCHAR(2000);
ALTER TABLE products ADD COLUMN IF NOT EXISTS stock INTEGER NOT NULL DEFAULT 0;
ALTER TABLE products ADD COLUMN IF NOT EXISTS image_url VARCHAR(500);
ALTER TABLE products ADD COLUMN IF NOT EXISTS average_rating DOUBLE PRECISION DEFAULT 0.0;
ALTER TABLE products ADD COLUMN IF NOT EXISTS review_count INTEGER DEFAULT 0;
ALTER TABLE products ADD COLUMN IF NOT EXISTS is_active BOOLEAN DEFAULT true;
ALTER TABLE products ADD COLUMN IF NOT EXISTS created_at TIMESTAMPTZ NOT NULL DEFAULT NOW();

-- Update existing products with default values
UPDATE products SET description = name || ' - A great product' WHERE description IS NULL;
UPDATE products SET stock = 100 WHERE stock IS NULL OR stock = 0;
UPDATE products SET is_active = true WHERE is_active IS NULL;
UPDATE products SET created_at = updated_at WHERE created_at IS NULL;

-- Add index on created_at
CREATE INDEX IF NOT EXISTS idx_products_created_at ON products(created_at DESC);
