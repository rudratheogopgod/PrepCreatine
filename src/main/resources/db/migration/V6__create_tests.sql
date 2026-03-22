-- V6: Create questions, test_sessions, and test_answers tables

-- ── Question Bank ─────────────────────────────────────────────────────────────
CREATE TABLE questions (
    id              UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    exam_id         VARCHAR(50)   NOT NULL,
    subject_id      VARCHAR(100)  NOT NULL,
    topic_id        VARCHAR(200)  NOT NULL,                     -- matches syllabus topic id
    level           SMALLINT      NOT NULL CHECK (level IN (1,2,3)),
    type            VARCHAR(20)   NOT NULL
                        CHECK (type IN ('mcq','integer','multi_correct')),
    question_text   TEXT          NOT NULL,
    option_a        VARCHAR(1000),
    option_b        VARCHAR(1000),
    option_c        VARCHAR(1000),
    option_d        VARCHAR(1000),
    correct_answer  VARCHAR(500)  NOT NULL,                     -- 'A','B','C','D' or integer
    explanation     TEXT,                                       -- shown after test submission
    is_ai_generated BOOLEAN       NOT NULL DEFAULT FALSE,
    source_ref      VARCHAR(200),                              -- year/paper if from real paper
    created_at      TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

CREATE TRIGGER trg_questions_updated_at
    BEFORE UPDATE ON questions
    FOR EACH ROW EXECUTE FUNCTION update_timestamp();

CREATE INDEX idx_questions_topic        ON questions (topic_id);
CREATE INDEX idx_questions_exam_subject ON questions (exam_id, subject_id);
CREATE INDEX idx_questions_level        ON questions (exam_id, level);

-- ── Test Sessions ─────────────────────────────────────────────────────────────
CREATE TABLE test_sessions (
    id               UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id          UUID          NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    exam_id          VARCHAR(50)   NOT NULL,
    test_type        VARCHAR(20)   NOT NULL
                         CHECK (test_type IN ('full_mock','topic_wise','rapid_fire')),
    subject_id       VARCHAR(100),                              -- null for full_mock
    topic_id         VARCHAR(200),                              -- null unless topic_wise
    level            SMALLINT      CHECK (level IN (1,2,3)),
    status           VARCHAR(20)   NOT NULL DEFAULT 'in_progress'
                         CHECK (status IN ('in_progress','submitted','abandoned')),
    total_questions  INTEGER       NOT NULL DEFAULT 0,
    answered_count   INTEGER       NOT NULL DEFAULT 0,
    correct_count    INTEGER       NOT NULL DEFAULT 0,
    score            NUMERIC(5,2),                              -- percentage 0.00-100.00
    time_taken_secs  INTEGER,
    started_at       TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    submitted_at     TIMESTAMPTZ,
    created_at       TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

CREATE TRIGGER trg_test_sessions_updated_at
    BEFORE UPDATE ON test_sessions
    FOR EACH ROW EXECUTE FUNCTION update_timestamp();

CREATE INDEX idx_test_sessions_user_id   ON test_sessions (user_id, created_at DESC);
CREATE INDEX idx_test_sessions_user_exam ON test_sessions (user_id, exam_id);

-- ── Test Answers (individual question responses) ─────────────────────────────
CREATE TABLE test_answers (
    id               UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    test_session_id  UUID        NOT NULL REFERENCES test_sessions(id) ON DELETE CASCADE,
    question_id      UUID        NOT NULL REFERENCES questions(id),
    user_answer      VARCHAR(500),                              -- null if unattempted
    is_correct       BOOLEAN,                                   -- null if unattempted
    time_taken_secs  INTEGER     NOT NULL DEFAULT 0,
    marked_review    BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_test_answers_session ON test_answers (test_session_id);
