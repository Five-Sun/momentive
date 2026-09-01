CREATE TABLE coupon (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    discount_type VARCHAR(20) NOT NULL,
    discount_value INTEGER NOT NULL,
    max_discount_amount INTEGER,
    min_order_amount INTEGER NOT NULL DEFAULT 0,
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE user_coupon (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    coupon_id BIGINT NOT NULL REFERENCES coupon(id),
    status VARCHAR(20) NOT NULL,
    used_order_id BIGINT,
    registered_at TIMESTAMP NOT NULL DEFAULT now(),
    used_at TIMESTAMP,
    CONSTRAINT uq_user_coupon UNIQUE (user_id, coupon_id)
);

CREATE INDEX idx_user_coupon_user_id ON user_coupon(user_id);
