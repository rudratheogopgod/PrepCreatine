-- V18: Add SM-2 spaced repetition columns to user_topic_progress
-- These drive the "Review Due Today" widget and the SM-2 scheduling algorithm.

ALTER TABLE user_topic_progress
    ADD COLUMN IF NOT EXISTS easiness_factor  NUMERIC(4,2)  NOT NULL DEFAULT 2.5,
    ADD COLUMN IF NOT EXISTS repetition_count INTEGER        NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS interval_days    INTEGER        NOT NULL DEFAULT 1,
    ADD COLUMN IF NOT EXISTS next_review_date DATE           NULL,
    ADD COLUMN IF NOT EXISTS last_reviewed_at TIMESTAMPTZ   NULL;

-- Partial index for efficient "review due today" queries
CREATE INDEX IF NOT EXISTS idx_utp_review_due
    ON user_topic_progress (user_id, next_review_date)
    WHERE next_review_date IS NOT NULL;

-- Also add test_type column to test_sessions if missing
ALTER TABLE test_sessions
    ADD COLUMN IF NOT EXISTS test_type VARCHAR(30)
        CHECK (test_type IN ('full_mock','topic_wise','rapid_fire','targeted_practice'));

-- Add level column to test_sessions
ALTER TABLE test_sessions
    ADD COLUMN IF NOT EXISTS level SMALLINT NOT NULL DEFAULT 2;
