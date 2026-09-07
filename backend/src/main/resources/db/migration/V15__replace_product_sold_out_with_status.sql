-- 품절 여부(sold_out)를 판매 상태(status)로 대체한다.
-- sold_out = TRUE였던 상품은 V5에서 이미 stock = 0으로 옮겨졌고 V14에서 재고 0인 variant로
-- 이관됐으므로, 품절은 재고 합에서 파생 판정된다. 따라서 기존 행은 전부 ON_SALE로 둔다.

ALTER TABLE product ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'ON_SALE';

UPDATE product SET status = 'ON_SALE';

-- 기존 행을 채우기 위한 임시 DEFAULT였으므로 제거한다(V3의 category 컬럼과 같은 방식).
-- 이후 INSERT는 애플리케이션이 status를 항상 명시한다.
ALTER TABLE product ALTER COLUMN status DROP DEFAULT;

ALTER TABLE product DROP COLUMN sold_out;
