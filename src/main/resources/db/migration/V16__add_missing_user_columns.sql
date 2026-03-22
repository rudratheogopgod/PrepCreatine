-- V16: Add missing columns to users table
-- These columns exist in the User entity but were missing from the V1 migration

ALTER TABLE users
    -- Exam & study preferences
    ADD COLUMN IF NOT EXISTS exam_type       VARCHAR(50),
    ADD COLUMN IF NOT EXISTS exam_date       DATE,
    ADD COLUMN IF NOT EXISTS study_mode      VARCHAR(30),
    ADD COLUMN IF NOT EXISTS daily_goal_mins INTEGER DEFAULT 60,
    -- Streak & activity tracking
    ADD COLUMN IF NOT EXISTS current_streak  INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS longest_streak  INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS total_days      INTEGER NOT NULL DEFAULT 0,
    -- Readiness score (0-100)
    ADD COLUMN IF NOT EXISTS readiness_score INTEGER NOT NULL DEFAULT 0,
    -- Mentor invite code (only for MENTOR role)
    ADD COLUMN IF NOT EXISTS mentor_code     VARCHAR(20) UNIQUE;
