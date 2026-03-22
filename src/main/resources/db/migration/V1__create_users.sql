-- V1: Create users table and update_timestamp trigger function
-- This trigger function is reused by ALL subsequent tables.

CREATE OR REPLACE FUNCTION update_timestamp()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TABLE users (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email               VARCHAR(254) NOT NULL,
    password_hash       VARCHAR(128) NOT NULL,
    full_name           VARCHAR(100) NOT NULL,
    role                VARCHAR(20)  NOT NULL DEFAULT 'STUDENT'
                            CHECK (role IN ('STUDENT', 'MENTOR', 'ADMIN')),
    is_email_verified   BOOLEAN      NOT NULL DEFAULT FALSE,
    onboarding_complete BOOLEAN      NOT NULL DEFAULT FALSE,
    is_active           BOOLEAN      NOT NULL DEFAULT TRUE,   -- soft delete flag
    avatar_url          VARCHAR(500),
    share_token         VARCHAR(64)  UNIQUE,                  -- public progress share link
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_users_email UNIQUE (email)
);

CREATE TRIGGER trg_users_updated_at
    BEFORE UPDATE ON users
    FOR EACH ROW EXECUTE FUNCTION update_timestamp();

CREATE INDEX idx_users_email      ON users (email);
CREATE INDEX idx_users_share_token ON users (share_token) WHERE share_token IS NOT NULL;
