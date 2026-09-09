-- 관리자 배송상태·송장 관리를 위한 컬럼 추가. 값이 없을 수 있는 주문(PENDING/FAILED/CANCELLED,
-- 그리고 이 마이그레이션 이전에 결제 완료된 주문 중 아직 배송 정보가 없는 경우)이 있어 전부 NULL 허용한다.
ALTER TABLE orders ADD COLUMN shipping_status VARCHAR(20) NULL;
ALTER TABLE orders ADD COLUMN courier VARCHAR(50) NULL;
ALTER TABLE orders ADD COLUMN tracking_number VARCHAR(100) NULL;

-- 이미 결제 완료(PAID)된 기존 주문은 배송준비중으로 백필한다. 실제 송장 정보는 없으므로
-- 관리자가 이후 수동으로 채워 넣어야 한다.
UPDATE orders SET shipping_status = 'PREPARING' WHERE status = 'PAID';
