ALTER TABLE orders
ADD COLUMN IF NOT EXISTS idempotency_key VARCHAR(120);

CREATE UNIQUE INDEX IF NOT EXISTS idx_orders_user_idempotency
ON orders (user_id, idempotency_key)
WHERE idempotency_key IS NOT NULL;
