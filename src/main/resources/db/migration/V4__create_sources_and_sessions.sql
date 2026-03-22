-- V4: Create sources and sessions tables

-- ── Sources (uploaded/ingested study material) ──────────────────────────────
CREATE TABLE sources (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    type            VARCHAR(20) NOT NULL
                        CHECK (type IN ('url','pdf','text')),
    title           VARCHAR(500) NOT NULL,
    original_url    VARCHAR(2048),                             -- for URL sources
    raw_text        TEXT,                                      -- extracted text
    status          VARCHAR(20) NOT NULL DEFAULT 'pending'
                        CHECK (status IN ('pending','processing','ready','failed')),
    error_message   VARCHAR(500),                              -- if status = 'failed'
    topic_count     INTEGER     NOT NULL DEFAULT 0,            -- extracted topic count
    study_guide     JSONB,                                     -- AI-generated study guide
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TRIGGER trg_sources_updated_at
    BEFORE UPDATE ON sources
    FOR EACH ROW EXECUTE FUNCTION update_timestamp();

CREATE INDEX idx_sources_user_id ON sources (user_id);
CREATE INDEX idx_sources_status  ON sources (status);
CREATE INDEX idx_sources_study_guide ON sources USING GIN (study_guide);

-- ── Sessions (daily study activity log) ─────────────────────────────────────
CREATE TABLE sessions (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    date            DATE        NOT NULL,
    duration_mins   INTEGER     NOT NULL DEFAULT 0,
    mode_used       VARCHAR(30),
    topics_touched  TEXT[]      NOT NULL DEFAULT '{}',
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_sessions_user_date UNIQUE (user_id, date)   -- required for UPSERT
);

CREATE TRIGGER trg_sessions_updated_at
    BEFORE UPDATE ON sessions
    FOR EACH ROW EXECUTE FUNCTION update_timestamp();

CREATE INDEX idx_sessions_user_date ON sessions (user_id, date DESC);
