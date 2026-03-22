-- V21: daily_plans table for StudyPlannerService cache
-- Stores AI-generated daily study plans, one per user per day
-- Cached so the same plan is served without re-calling Gemini

CREATE TABLE IF NOT EXISTS daily_plans (
    id               UUID         DEFAULT gen_random_uuid() PRIMARY KEY,
    user_id          UUID         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    plan_date        DATE         NOT NULL,
    total_minutes    INTEGER      NOT NULL DEFAULT 120,
    sessions_json    JSONB        NOT NULL DEFAULT '[]'::jsonb,
    motivation_msg   TEXT,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    UNIQUE (user_id, plan_date)
);

CREATE INDEX IF NOT EXISTS idx_daily_plans_user_date
    ON daily_plans (user_id, plan_date DESC);
