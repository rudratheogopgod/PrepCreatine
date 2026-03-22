-- V14: Add columns missing from entities but absent from earlier migrations

-- ── test_sessions: add question_ids (jsonb) and time_limit_mins ────────────────
ALTER TABLE test_sessions
    ADD COLUMN IF NOT EXISTS question_ids    JSONB,
    ADD COLUMN IF NOT EXISTS time_limit_mins INTEGER;

-- ── user_topic_progress: add last_score and test_attempts ─────────────────────
ALTER TABLE user_topic_progress
    ADD COLUMN IF NOT EXISTS last_score    NUMERIC(5,2),
    ADD COLUMN IF NOT EXISTS test_attempts INTEGER NOT NULL DEFAULT 0;

-- ── sessions: duration_mins already exists from V4, no change needed ──────────
-- Entity was updated to reference duration_mins (not study_mins).
