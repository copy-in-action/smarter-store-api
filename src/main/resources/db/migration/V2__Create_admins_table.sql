-- Admin 테이블 생성
CREATE TABLE admins (
    id BIGSERIAL PRIMARY KEY,
    login_id VARCHAR(255) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);
