-- V13__Create_email_verification_table.sql

-- This script creates the email_verification_tokens table and related objects.

-- Comments for the table and its columns
COMMENT ON TABLE email_verification_tokens IS 'Table to store email verification tokens';
COMMENT ON COLUMN email_verification_tokens.id IS 'Unique identifier for the token';
COMMENT ON COLUMN email_verification_tokens.user_id IS 'Foreign key referencing the user';
COMMENT ON COLUMN email_verification_tokens.token IS 'The verification token string';
COMMENT ON COLUMN email_verification_tokens.status IS 'The status of the token (e.g., VERIFIED, UNVERIFIED, EXPIRED)';
COMMENT ON COLUMN email_verification_tokens.expires_at IS 'The expiration timestamp for the token';
COMMENT ON COLUMN email_verification_tokens.created_at IS 'Timestamp of when the token was created';
COMMENT ON COLUMN email_verification_tokens.updated_at IS 'Timestamp of when the token was last updated';

-- Create the ENUM type for token status
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'verification_token_status') THEN
        CREATE TYPE verification_token_status AS ENUM (
            'VERIFIED',
            'UNVERIFIED',
            'EXPIRED'
        );
    END IF;
END$$;

-- Create the table for email verification tokens
CREATE TABLE email_verification_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    token VARCHAR(255) NOT NULL,
    status verification_token_status NOT NULL DEFAULT 'UNVERIFIED',
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_user_id FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Additional comments and index creation
COMMENT ON CONSTRAINT fk_user_id ON email_verification_tokens IS 'Ensures that the user_id in email_verification_tokens corresponds to a valid user.';
CREATE INDEX idx_email_verification_tokens_token ON email_verification_tokens(token);
COMMENT ON INDEX idx_email_verification_tokens_token IS 'Index on the token column for faster lookups.';
