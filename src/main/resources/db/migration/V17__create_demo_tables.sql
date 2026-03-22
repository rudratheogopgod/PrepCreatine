-- V17__create_demo_tables.sql
-- Creates tables used by DemoUserSeeder that weren't in prior migrations.
-- Also used in production for daily plan and concept graph features.

-- ── Daily Plans ──────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS daily_plans (
    id            UUID                     DEFAULT gen_random_uuid() PRIMARY KEY,
    user_id       UUID                     NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    plan_date     DATE                     NOT NULL,
    total_minutes INT                      NOT NULL DEFAULT 0,
    sessions_json JSONB                    NOT NULL DEFAULT '[]',
    motivation_msg TEXT,
    created_at    TIMESTAMPTZ              NOT NULL DEFAULT NOW(),
    CONSTRAINT daily_plans_user_date_uq UNIQUE (user_id, plan_date)
);

CREATE INDEX IF NOT EXISTS idx_daily_plans_user_date
  ON daily_plans (user_id, plan_date DESC);

-- ── Concept Graph Nodes ───────────────────────────────────────────────────────
-- Tracks per-user, per-topic sub-concept mastery for knowledge graph visualization.
CREATE TABLE IF NOT EXISTS concept_graph_nodes (
    id         UUID        DEFAULT gen_random_uuid() PRIMARY KEY,
    user_id    UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    topic_id   TEXT        NOT NULL,
    concept    TEXT        NOT NULL,
    mastery    FLOAT       NOT NULL DEFAULT 0.0 CHECK (mastery BETWEEN 0 AND 1),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT concept_graph_nodes_user_topic_concept_uq
        UNIQUE (user_id, topic_id, concept)
);

CREATE INDEX IF NOT EXISTS idx_concept_graph_user_topic
  ON concept_graph_nodes (user_id, topic_id);

-- ── Sessions: unique constraint already exists from V4 ──────────────────────
-- Nothing to alter here — mode_used and topics_touched were added in V4.
