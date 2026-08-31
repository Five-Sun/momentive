ALTER TABLE orders ADD COLUMN shipping_fee INTEGER;

UPDATE orders SET shipping_fee = 0 WHERE shipping_fee IS NULL;

ALTER TABLE orders ALTER COLUMN shipping_fee SET NOT NULL;
