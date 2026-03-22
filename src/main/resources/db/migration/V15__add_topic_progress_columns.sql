-- V15: Add missing columns to user_topic_progress

ALTER TABLE user_topic_progress
    ADD COLUMN IF NOT EXISTS last_score    NUMERIC(5,2),
    ADD COLUMN IF NOT EXISTS test_attempts INTEGER NOT NULL DEFAULT 0;
