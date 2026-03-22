-- V3: Create user_topic_progress table

CREATE TABLE user_topic_progress (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    topic_id        VARCHAR(200) NOT NULL,                     -- matches static syllabus id
    exam_id         VARCHAR(50)  NOT NULL,                     -- 'jee', 'neet', etc.
    subject_id      VARCHAR(100) NOT NULL,
    status          VARCHAR(20)  NOT NULL DEFAULT 'not_started'
                        CHECK (status IN ('not_started','in_progress','done')),
    time_spent_mins INTEGER      NOT NULL DEFAULT 0,
    last_touched    TIMESTAMPTZ,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_utp_user_topic UNIQUE (user_id, topic_id)   -- safe for UPSERT
);

CREATE TRIGGER trg_utp_updated_at
    BEFORE UPDATE ON user_topic_progress
    FOR EACH ROW EXECUTE FUNCTION update_timestamp();

CREATE INDEX idx_utp_user_id   ON user_topic_progress (user_id);
CREATE INDEX idx_utp_user_exam ON user_topic_progress (user_id, exam_id);
CREATE INDEX idx_utp_status    ON user_topic_progress (user_id, status);
