ALTER TABLE orders
    ADD COLUMN items_subtotal INTEGER,
    ADD COLUMN discount_amount INTEGER,
    ADD COLUMN user_coupon_id BIGINT REFERENCES user_coupon(id);

-- 기존 주문은 할인 개념이 없었으므로 items_subtotal을 total_amount - shipping_fee로 역산해 백필하고,
-- discount_amount는 0, user_coupon_id는 NULL로 채운다.
UPDATE orders
SET items_subtotal = total_amount - shipping_fee,
    discount_amount = 0
WHERE items_subtotal IS NULL;

ALTER TABLE orders
    ALTER COLUMN items_subtotal SET NOT NULL,
    ALTER COLUMN discount_amount SET NOT NULL,
    ALTER COLUMN discount_amount SET DEFAULT 0;
