CREATE TABLE IF NOT EXISTS products (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    category VARCHAR(80) NOT NULL,
    price NUMERIC(10, 2) NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_products_category ON products (category);
CREATE INDEX IF NOT EXISTS idx_products_updated_at ON products (updated_at DESC);

CREATE TABLE IF NOT EXISTS orders (
    id BIGSERIAL PRIMARY KEY,
    user_id VARCHAR(120) NOT NULL,
    product_id BIGINT NOT NULL REFERENCES products(id),
    quantity INT NOT NULL CHECK (quantity > 0),
    total_amount NUMERIC(12, 2) NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_orders_user_id ON orders (user_id);
CREATE INDEX IF NOT EXISTS idx_orders_created_at ON orders (created_at DESC);

CREATE TABLE IF NOT EXISTS app_users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_app_users_email ON app_users (email);

INSERT INTO products (name, category, price)
SELECT * FROM (
    VALUES
        ('Laptop Pro 14', 'electronics', 1299.99),
        ('Wireless Earbuds', 'electronics', 149.99),
        ('Mechanical Keyboard', 'accessories', 99.99),
        ('Ergonomic Chair', 'furniture', 349.99),
        ('4K Monitor', 'electronics', 499.99),
        ('Desk Lamp', 'furniture', 39.99),
        ('Smart Watch', 'wearables', 229.99),
        ('Travel Backpack', 'lifestyle', 79.99),
        ('Coffee Maker', 'home', 119.99),
        ('Gaming Mouse', 'accessories', 59.99)
) AS seed(name, category, price)
WHERE NOT EXISTS (SELECT 1 FROM products);
