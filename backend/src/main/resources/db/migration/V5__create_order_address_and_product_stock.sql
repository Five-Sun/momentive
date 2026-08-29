ALTER TABLE product ADD COLUMN stock INTEGER;
ALTER TABLE product ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

UPDATE product SET stock = 0 WHERE sold_out = TRUE;
UPDATE product SET stock = 100 WHERE sold_out = FALSE;

ALTER TABLE product ALTER COLUMN stock SET NOT NULL;

CREATE TABLE address (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    recipient VARCHAR(100) NOT NULL,
    phone VARCHAR(20) NOT NULL,
    zipcode VARCHAR(10) NOT NULL,
    address1 VARCHAR(255) NOT NULL,
    address2 VARCHAR(255),
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_address_user_id ON address(user_id);

CREATE TABLE orders (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    status VARCHAR(20) NOT NULL,
    total_amount INTEGER NOT NULL,
    address_id BIGINT NOT NULL REFERENCES address(id),
    toss_payment_key VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_orders_user_id ON orders(user_id);
CREATE INDEX idx_orders_status ON orders(status);

CREATE TABLE order_item (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL REFERENCES orders(id),
    product_id BIGINT NOT NULL REFERENCES product(id),
    quantity INTEGER NOT NULL,
    size VARCHAR(50),
    unit_price INTEGER NOT NULL
);

CREATE INDEX idx_order_item_order_id ON order_item(order_id);
