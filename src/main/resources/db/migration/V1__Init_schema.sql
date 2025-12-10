-- V1__Init_schema.sql
-- 기본 스키마: users, refresh_tokens, admins, venue, performance, performance_schedule, ticket_option

-- Users 테이블
CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    username VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

COMMENT ON TABLE users IS '사용자 정보';
COMMENT ON COLUMN users.id IS '사용자 ID';
COMMENT ON COLUMN users.email IS '이메일';
COMMENT ON COLUMN users.username IS '사용자 이름';
COMMENT ON COLUMN users.password_hash IS '비밀번호 해시';
COMMENT ON COLUMN users.role IS '역할 (USER, ADMIN)';
COMMENT ON COLUMN users.created_at IS '생성일시';
COMMENT ON COLUMN users.updated_at IS '수정일시';

-- Refresh Token 테이블
CREATE TABLE IF NOT EXISTS refresh_tokens (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    token VARCHAR(255) NOT NULL UNIQUE,
    expiry_date TIMESTAMP NOT NULL,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

COMMENT ON TABLE refresh_tokens IS '리프레시 토큰';
COMMENT ON COLUMN refresh_tokens.id IS '토큰 ID';
COMMENT ON COLUMN refresh_tokens.user_id IS '사용자 ID (FK)';
COMMENT ON COLUMN refresh_tokens.token IS '토큰 값';
COMMENT ON COLUMN refresh_tokens.expiry_date IS '만료일시';
COMMENT ON COLUMN refresh_tokens.created_at IS '생성일시';
COMMENT ON COLUMN refresh_tokens.updated_at IS '수정일시';

-- Admin 테이블
CREATE TABLE IF NOT EXISTS admins (
    id BIGSERIAL PRIMARY KEY,
    login_id VARCHAR(255) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

COMMENT ON TABLE admins IS '관리자 정보';
COMMENT ON COLUMN admins.id IS '관리자 ID';
COMMENT ON COLUMN admins.login_id IS '로그인 ID';
COMMENT ON COLUMN admins.name IS '이름';
COMMENT ON COLUMN admins.password_hash IS '비밀번호 해시';
COMMENT ON COLUMN admins.created_at IS '생성일시';
COMMENT ON COLUMN admins.updated_at IS '수정일시';

-- Venue 테이블
CREATE TABLE IF NOT EXISTS venue (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    address VARCHAR(255),
    seating_chart_url VARCHAR(255),
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

COMMENT ON TABLE venue IS '공연장 정보';
COMMENT ON COLUMN venue.id IS '공연장 ID';
COMMENT ON COLUMN venue.name IS '공연장 이름';
COMMENT ON COLUMN venue.address IS '주소';
COMMENT ON COLUMN venue.seating_chart_url IS '좌석 배치도 URL';
COMMENT ON COLUMN venue.created_at IS '생성일시';
COMMENT ON COLUMN venue.updated_at IS '수정일시';

-- Performance 테이블
CREATE TABLE IF NOT EXISTS performance (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    category VARCHAR(50) NOT NULL,
    running_time INT,
    age_rating VARCHAR(50),
    main_image_url VARCHAR(255),
    venue_id BIGINT,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    visible BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    FOREIGN KEY (venue_id) REFERENCES venue (id)
);

COMMENT ON TABLE performance IS '공연 정보';
COMMENT ON COLUMN performance.id IS '공연 ID';
COMMENT ON COLUMN performance.title IS '공연명';
COMMENT ON COLUMN performance.description IS '상세 설명';
COMMENT ON COLUMN performance.category IS '카테고리';
COMMENT ON COLUMN performance.running_time IS '러닝타임 (분)';
COMMENT ON COLUMN performance.age_rating IS '관람 연령';
COMMENT ON COLUMN performance.main_image_url IS '대표 이미지 URL';
COMMENT ON COLUMN performance.venue_id IS '공연장 ID (FK)';
COMMENT ON COLUMN performance.start_date IS '공연 시작일';
COMMENT ON COLUMN performance.end_date IS '공연 종료일';
COMMENT ON COLUMN performance.visible IS '노출 여부';
COMMENT ON COLUMN performance.created_at IS '생성일시';
COMMENT ON COLUMN performance.updated_at IS '수정일시';

-- Performance Schedule 테이블
CREATE TABLE IF NOT EXISTS performance_schedule (
    id BIGSERIAL PRIMARY KEY,
    performance_id BIGINT NOT NULL,
    show_datetime TIMESTAMP NOT NULL,
    sale_start_datetime TIMESTAMP,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    FOREIGN KEY (performance_id) REFERENCES performance (id) ON DELETE CASCADE
);

COMMENT ON TABLE performance_schedule IS '공연 회차';
COMMENT ON COLUMN performance_schedule.id IS '회차 ID';
COMMENT ON COLUMN performance_schedule.performance_id IS '공연 ID (FK)';
COMMENT ON COLUMN performance_schedule.show_datetime IS '공연 일시';
COMMENT ON COLUMN performance_schedule.sale_start_datetime IS '판매 시작 일시';
COMMENT ON COLUMN performance_schedule.created_at IS '생성일시';
COMMENT ON COLUMN performance_schedule.updated_at IS '수정일시';

-- Ticket Option 테이블
CREATE TABLE IF NOT EXISTS ticket_option (
    id BIGSERIAL PRIMARY KEY,
    performance_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    price DECIMAL(10, 2) NOT NULL,
    total_quantity INT,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    FOREIGN KEY (performance_id) REFERENCES performance (id) ON DELETE CASCADE
);

COMMENT ON TABLE ticket_option IS '티켓 옵션';
COMMENT ON COLUMN ticket_option.id IS '옵션 ID';
COMMENT ON COLUMN ticket_option.performance_id IS '공연 ID (FK)';
COMMENT ON COLUMN ticket_option.name IS '좌석 등급명';
COMMENT ON COLUMN ticket_option.price IS '가격';
COMMENT ON COLUMN ticket_option.total_quantity IS '총 수량';
COMMENT ON COLUMN ticket_option.created_at IS '생성일시';
COMMENT ON COLUMN ticket_option.updated_at IS '수정일시';
