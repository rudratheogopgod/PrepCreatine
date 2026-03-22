-- V19: System NCERT corpus tables (separate from per-user sources)
-- Also: YouTube support columns on sources, grounded_in on chat messages

-- ── sources: add YouTube columns ──────────────────────────────────────────────
ALTER TABLE sources
    ADD COLUMN IF NOT EXISTS thumbnail_url VARCHAR(500),
    ADD COLUMN IF NOT EXISTS video_id      VARCHAR(50);

-- Update sources type CHECK to include YOUTUBE
ALTER TABLE sources DROP CONSTRAINT IF EXISTS sources_type_check;
ALTER TABLE sources
    ADD CONSTRAINT sources_type_check
    CHECK (type IN ('url','pdf','text','YOUTUBE','URL','PDF','TEXT'));

-- ── messages: add grounded_in citations array ─────────────────────────────────
ALTER TABLE messages
    ADD COLUMN IF NOT EXISTS grounded_in TEXT[] NOT NULL DEFAULT '{}';

-- ── System NCERT corpus (read-only, shared across all users) ─────────────────
CREATE TABLE IF NOT EXISTS system_sources (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    exam_id    VARCHAR(50)  NOT NULL,
    subject_id VARCHAR(100) NOT NULL,
    title      VARCHAR(500) NOT NULL,
    raw_text   TEXT         NOT NULL,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_sys_sources_exam ON system_sources(exam_id, subject_id);

CREATE TABLE IF NOT EXISTS system_source_chunks (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    source_id   UUID         NOT NULL REFERENCES system_sources(id) ON DELETE CASCADE,
    exam_id     VARCHAR(50)  NOT NULL,
    subject_id  VARCHAR(100) NOT NULL,
    chunk_index INTEGER      NOT NULL,
    chunk_text  TEXT         NOT NULL,
    embedding   vector(768),
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_ssc_exam ON system_source_chunks(exam_id, subject_id);
-- ivfflat index for approximate vector search (requires at least 1 row to build)
-- Using IF NOT EXISTS guard to avoid error on empty table
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_indexes
        WHERE tablename = 'system_source_chunks'
          AND indexname = 'idx_ssc_embedding'
    ) THEN
        CREATE INDEX idx_ssc_embedding ON system_source_chunks
            USING ivfflat (embedding vector_cosine_ops) WITH (lists = 10);
    END IF;
END$$;
