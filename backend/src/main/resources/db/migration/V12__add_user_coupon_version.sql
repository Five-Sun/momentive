-- 쿠폰 선점(USED 전이)의 동시성 제어를 위한 낙관적 락 컬럼.
-- product.version과 동일한 방식으로, 같은 쿠폰에 대한 동시 주문에서 한쪽만 성공하도록 만든다.
ALTER TABLE user_coupon ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
