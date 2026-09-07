-- 신규 주문부터 어떤 variant에서 재고를 뺐는지 기록한다.
-- 기존 행은 건드리지 않는다: 과거 주문의 size 문자열("S" 등)이 어느 variant인지 정할 방법이 없어
-- 소급 매핑은 spec의 Out of Scope다. 따라서 이 마이그레이션 이전에 생성된 행의
-- variant_id는 전부 NULL로 남고, size 스냅샷 문자열이 그대로 주문 이력을 표시한다.
ALTER TABLE order_item ADD COLUMN variant_id BIGINT REFERENCES product_variant(id);

CREATE INDEX idx_order_item_variant_id ON order_item(variant_id);
