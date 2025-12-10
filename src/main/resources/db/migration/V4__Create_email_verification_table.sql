-- V4__Create_email_verification_table.sql
-- 이메일 인증 토큰 테이블

CREATE TABLE IF NOT EXISTS email_verification_tokens (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    token VARCHAR(255) NOT NULL UNIQUE,
    expiry_date TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_email_verification_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT uk_email_verification_user UNIQUE (user_id)
);

COMMENT ON TABLE email_verification_tokens IS '이메일 인증 토큰';
COMMENT ON COLUMN email_verification_tokens.id IS 'ID';
COMMENT ON COLUMN email_verification_tokens.user_id IS '사용자 ID (FK)';
COMMENT ON COLUMN email_verification_tokens.token IS '토큰';
COMMENT ON COLUMN email_verification_tokens.expiry_date IS '만료일시';
COMMENT ON COLUMN email_verification_tokens.created_at IS '생성일시';
COMMENT ON COLUMN email_verification_tokens.updated_at IS '수정일시';

CREATE INDEX IF NOT EXISTS idx_email_verification_tokens_token ON email_verification_tokens(token);
