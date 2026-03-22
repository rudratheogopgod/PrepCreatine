-- V2: Create user_contexts table

CREATE TABLE user_contexts (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    exam_type       VARCHAR(50),                               -- 'jee', 'neet', 'gate-cs', etc.
    exam_date       DATE,
    study_mode      VARCHAR(30) NOT NULL DEFAULT 'in_depth'
                        CHECK (study_mode IN ('in_depth','revision','overview','speed_run')),
    daily_goal_mins INTEGER     NOT NULL DEFAULT 90
                        CHECK (daily_goal_mins BETWEEN 15 AND 600),
    weak_topics     TEXT[]      NOT NULL DEFAULT '{}',         -- array of topic IDs
    strong_topics   TEXT[]      NOT NULL DEFAULT '{}',
    custom_topic    VARCHAR(200),                              -- Path C (speed_run) users
    theme           VARCHAR(10) NOT NULL DEFAULT 'system'
                        CHECK (theme IN ('light','dark','system')),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_user_contexts_user_id UNIQUE (user_id)      -- one context per user
);

CREATE TRIGGER trg_user_contexts_updated_at
    BEFORE UPDATE ON user_contexts
    FOR EACH ROW EXECUTE FUNCTION update_timestamp();

CREATE INDEX idx_user_contexts_user_id ON user_contexts (user_id);

-- GIN indexes for array columns (enables fast @> operator queries)
CREATE INDEX idx_user_contexts_weak_topics   ON user_contexts USING GIN (weak_topics);
CREATE INDEX idx_user_contexts_strong_topics ON user_contexts USING GIN (strong_topics);
