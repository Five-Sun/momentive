CREATE TABLE product (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT NOT NULL,
    price INTEGER NOT NULL,
    discount_price INTEGER,
    sold_out BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE product_image (
    id BIGSERIAL PRIMARY KEY,
    product_id BIGINT NOT NULL REFERENCES product(id),
    url VARCHAR(1000) NOT NULL,
    display_order INTEGER NOT NULL
);

CREATE INDEX idx_product_image_product_id ON product_image(product_id);
CREATE INDEX idx_product_created_at ON product(created_at);
