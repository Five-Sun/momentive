CREATE TABLE review (
    id BIGSERIAL PRIMARY KEY,
    product_id BIGINT NOT NULL REFERENCES product(id),
    user_id BIGINT NOT NULL REFERENCES users(id),
    rating INTEGER NOT NULL,
    text VARCHAR(500) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now(),
    UNIQUE (product_id, user_id)
);

CREATE INDEX idx_review_product_id ON review(product_id);
CREATE INDEX idx_review_user_id ON review(user_id);

ALTER TABLE product ADD COLUMN average_rating DOUBLE PRECISION;
ALTER TABLE product ADD COLUMN review_count INTEGER NOT NULL DEFAULT 0;
