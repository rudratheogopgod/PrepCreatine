-- V13: Create all composite, partial, and additional indexes

-- ── GIN indexes for JSONB and array columns ───────────────────────────────────
-- (GIN indexes on TEXT[] and JSONB enable efficient @>, ?, and ?| operators)
-- Already created for study_guide in V4 and weak/strong_topics in V2.

-- ── Full-text search support via pg_trgm ─────────────────────────────────────
CREATE INDEX idx_threads_title_trgm
    ON community_threads USING GIN (title gin_trgm_ops);

CREATE INDEX idx_questions_text_trgm
    ON questions USING GIN (question_text gin_trgm_ops);

-- ── Additional performance indexes ───────────────────────────────────────────

-- Fast lookup of in-progress test sessions for a user
CREATE INDEX idx_test_sessions_in_progress
    ON test_sessions (user_id, status)
    WHERE status = 'in_progress';

-- Fast lookup of pending/processing sources
CREATE INDEX idx_sources_pending
    ON sources (status)
    WHERE status IN ('pending', 'processing');

-- Fast lookup of unaccepted community answers for a thread
CREATE INDEX idx_answers_unaccepted
    ON community_answers (thread_id, is_accepted)
    WHERE is_accepted = FALSE;

-- Composite index for analytics: session history per user ordered by date
CREATE INDEX idx_sessions_user_date_desc
    ON sessions (user_id, date DESC);

-- Source chunk lookup by source + index (for ordered chunk retrieval)
CREATE INDEX idx_sc_source_chunk_index
    ON source_chunks (source_id, chunk_index ASC);

-- Notification creation time for feed ordering
CREATE INDEX idx_notifications_created
    ON notifications (user_id, created_at DESC);
