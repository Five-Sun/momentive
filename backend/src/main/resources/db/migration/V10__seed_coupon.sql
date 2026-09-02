INSERT INTO coupon (code, name, discount_type, discount_value, max_discount_amount, min_order_amount, expires_at, created_at) VALUES
('WELCOME3000', '웰컴 3,000원 할인', 'FIXED', 3000, NULL, 30000, '2026-12-31 23:59:59', now()),
('MOMENTIVE10', '전 상품 10% 할인', 'PERCENT', 10, 5000, 0, '2026-12-31 23:59:59', now()),
('FIRSTORDER5000', '첫 구매 5,000원 할인', 'FIXED', 5000, NULL, 0, '2026-12-31 23:59:59', now()),
('VIP20', 'VIP 20% 할인', 'PERCENT', 20, 10000, 50000, '2026-12-31 23:59:59', now());
