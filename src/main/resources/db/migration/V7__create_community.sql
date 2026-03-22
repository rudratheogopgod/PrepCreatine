-- V7: Create community tables (threads, answers, upvotes)

-- ── Community Threads (Q&A posts) ────────────────────────────────────────────
CREATE TABLE community_threads (
    id              UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    exam_id         VARCHAR(50)  NOT NULL,
    subject_id      VARCHAR(100),
    topic_id        VARCHAR(200),
    title           VARCHAR(300) NOT NULL,
    body            TEXT,
    upvote_count    INTEGER      NOT NULL DEFAULT 0,
    answer_count    INTEGER      NOT NULL DEFAULT 0,
    is_resolved     BOOLEAN      NOT NULL DEFAULT FALSE,
    ai_summary      TEXT,                                       -- generated when >5 answers
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE TRIGGER trg_community_threads_updated_at
    BEFORE UPDATE ON community_threads
    FOR EACH ROW EXECUTE FUNCTION update_timestamp();

CREATE INDEX idx_threads_exam        ON community_threads (exam_id);
CREATE INDEX idx_threads_user        ON community_threads (user_id);
CREATE INDEX idx_threads_topic       ON community_threads (topic_id) WHERE topic_id IS NOT NULL;
CREATE INDEX idx_threads_created     ON community_threads (exam_id, created_at DESC);

-- ── Community Answers ─────────────────────────────────────────────────────────
CREATE TABLE community_answers (
    id               UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    thread_id        UUID        NOT NULL REFERENCES community_threads(id) ON DELETE CASCADE,
    user_id          UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    body             TEXT        NOT NULL,
    upvote_count     INTEGER     NOT NULL DEFAULT 0,
    is_accepted      BOOLEAN     NOT NULL DEFAULT FALSE,
    is_mentor_answer BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TRIGGER trg_community_answers_updated_at
    BEFORE UPDATE ON community_answers
    FOR EACH ROW EXECUTE FUNCTION update_timestamp();

CREATE INDEX idx_answers_thread ON community_answers (thread_id, created_at ASC);

-- ── Community Upvotes (join table — prevents double-upvoting) ────────────────
CREATE TABLE community_upvotes (
    id           UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id      UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    entity_type  VARCHAR(10) NOT NULL CHECK (entity_type IN ('thread', 'answer')),
    entity_id    UUID        NOT NULL,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_upvote_user_entity UNIQUE (user_id, entity_type, entity_id)
);

CREATE INDEX idx_upvotes_entity ON community_upvotes (entity_type, entity_id);
