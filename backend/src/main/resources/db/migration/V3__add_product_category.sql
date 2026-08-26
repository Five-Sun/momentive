ALTER TABLE product ADD COLUMN category VARCHAR(20) NOT NULL DEFAULT 'ACCESSORY';

UPDATE product SET category = 'OUTER' WHERE name IN ('강아지 하네스 M', '강아지 방한 패딩', '강아지 우비');

ALTER TABLE product ALTER COLUMN category DROP DEFAULT;
