-- 1. created_at 컬럼 추가
ALTER TABLE product
    ADD COLUMN created_at TIMESTAMP NOT NULL DEFAULT NOW();

-- 2. updated_at 컬럼 추가
ALTER TABLE product
    ADD COLUMN updated_at TIMESTAMP NOT NULL DEFAULT NOW();
