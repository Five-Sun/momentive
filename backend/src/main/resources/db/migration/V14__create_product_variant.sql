-- 재고의 단위를 product에서 product_variant로 옮긴다.
-- 순서가 중요하다: 기존 product.stock 값을 variant로 INSERT한 뒤에만 컬럼을 DROP한다.
-- (DROP이 앞서면 이관할 값이 사라지고, 되돌릴 수 없다.)

CREATE TABLE product_variant (
    id BIGSERIAL PRIMARY KEY,
    product_id BIGINT NOT NULL REFERENCES product(id),
    size VARCHAR(50),
    stock INTEGER NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT chk_product_variant_stock_non_negative CHECK (stock >= 0)
);

CREATE INDEX idx_product_variant_product_id ON product_variant(product_id);

-- size가 nullable이라 UNIQUE(product_id, size) 하나로는 NULL 중복을 막지 못한다
-- (Postgres에서 NULL은 서로 같지 않은 값으로 취급되어 여러 행이 통과한다).
-- 부분 유니크 인덱스 두 개로 나눠 "사이즈가 있는 행은 상품 내 유일",
-- "사이즈가 없는 행은 상품당 최대 1개"를 각각 보장한다.
CREATE UNIQUE INDEX uq_product_variant_product_size
    ON product_variant(product_id, size) WHERE size IS NOT NULL;
CREATE UNIQUE INDEX uq_product_variant_product_no_size
    ON product_variant(product_id) WHERE size IS NULL;

-- 기존 상품 전체를 size = NULL 단일 variant로 이관하며 재고 수치를 그대로 옮긴다.
INSERT INTO product_variant (product_id, size, stock)
SELECT id, NULL, stock FROM product;

ALTER TABLE product DROP COLUMN stock;
