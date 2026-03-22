-- V9: Create notifications and email_verification_tokens tables

-- ── Notifications ─────────────────────────────────────────────────────────────
CREATE TABLE notifications (
    id          UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    type        VARCHAR(50)  NOT NULL,
    title       VARCHAR(200) NOT NULL,
    body        VARCHAR(500),
    is_read     BOOLEAN      NOT NULL DEFAULT FALSE,
    action_url  VARCHAR(500),
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
    -- No updated_at — notifications are not updated, only read/deleted
);

CREATE INDEX idx_notifications_user ON notifications (user_id, created_at DESC);
-- Partial index for fast unread count query
CREATE INDEX idx_notifications_unread ON notifications (user_id, is_read)
    WHERE is_read = FALSE;

-- ── Email Verification Tokens (shared for verify-email and password-reset) ───
CREATE TABLE email_verification_tokens (
    id          UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token       VARCHAR(128) NOT NULL,
    type        VARCHAR(20)  NOT NULL
                    CHECK (type IN ('email_verify','password_reset')),
    expires_at  TIMESTAMPTZ  NOT NULL,
    used_at     TIMESTAMPTZ,                                    -- null = not yet used
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_evt_token UNIQUE (token)
);

CREATE INDEX idx_evt_token   ON email_verification_tokens (token);
CREATE INDEX idx_evt_user_id ON email_verification_tokens (user_id);
