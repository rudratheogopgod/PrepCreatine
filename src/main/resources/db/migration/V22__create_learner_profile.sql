-- V22: Learner Profile + Concept Struggles
-- Persistent behavioral model for AI agent (BSDD AI Agent §A, §I)

CREATE TABLE IF NOT EXISTS learner_profiles (
    id                    UUID         DEFAULT gen_random_uuid() PRIMARY KEY,
    user_id               UUID         NOT NULL REFERENCES users(id) ON DELETE CASCADE,

    -- Behavioral patterns (updated after every test + study session)
    avg_time_per_correct  NUMERIC(6,2) NOT NULL DEFAULT 0,   -- seconds per correct answer
    avg_time_per_wrong    NUMERIC(6,2) NOT NULL DEFAULT 0,   -- seconds per wrong answer
    struggle_indicator    NUMERIC(4,3) NOT NULL DEFAULT 0,   -- 0=no struggle, 1=maximum
    consistency_score     NUMERIC(4,3) NOT NULL DEFAULT 0,   -- days_studied / 14

    -- AI-generated insight (written by LearnerAnalysisAgent)
    weakness_pattern      TEXT,
    strength_pattern      TEXT,
    recommended_mode      VARCHAR(30),   -- 'in_depth'|'revision'|'speed_run'|'overview'
    learning_velocity     NUMERIC(4,3) NOT NULL DEFAULT 0.5, -- 0=slow, 1=fast

    -- Metadata
    last_analyzed_at      TIMESTAMPTZ,
    total_study_sessions  INTEGER      NOT NULL DEFAULT 0,
    total_questions_seen  INTEGER      NOT NULL DEFAULT 0,
    created_at            TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at            TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    UNIQUE (user_id)
);

-- Concept-level struggle tracking (used by agentic RAG query enrichment)
CREATE TABLE IF NOT EXISTS concept_struggles (
    id              UUID         DEFAULT gen_random_uuid() PRIMARY KEY,
    user_id         UUID         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    topic_id        VARCHAR(200) NOT NULL,
    concept_tag     VARCHAR(200) NOT NULL,  -- e.g. "rate_of_change", "electron_movement"
    struggle_count  INTEGER      NOT NULL DEFAULT 1,
    last_seen_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    UNIQUE (user_id, concept_tag)
);

CREATE INDEX IF NOT EXISTS idx_learner_profiles_user
    ON learner_profiles (user_id);

CREATE INDEX IF NOT EXISTS idx_concept_struggles_user
    ON concept_struggles (user_id);

CREATE INDEX IF NOT EXISTS idx_concept_struggles_count
    ON concept_struggles (user_id, struggle_count DESC);
