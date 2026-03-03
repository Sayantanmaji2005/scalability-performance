-- Wishlist table
CREATE TABLE IF NOT EXISTS wishlists (
    id BIGSERIAL PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL,
    product_id BIGINT NOT NULL REFERENCES products(id),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_wishlist_product FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE,
    CONSTRAINT uk_wishlist_user_product UNIQUE (user_id, product_id)
);

CREATE INDEX idx_wishlist_user_id ON wishlists(user_id);
CREATE INDEX idx_wishlist_created_at ON wishlists(created_at DESC);
