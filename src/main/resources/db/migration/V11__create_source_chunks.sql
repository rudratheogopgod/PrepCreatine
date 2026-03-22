-- V11: Create source_chunks table with pgvector embedding column
-- Requires pgvector extension (enabled in V10)

CREATE TABLE source_chunks (
    id           UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    source_id    UUID         NOT NULL REFERENCES sources(id) ON DELETE CASCADE,
    user_id      UUID         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    chunk_index  INTEGER      NOT NULL,                         -- position in document (0-based)
    content      TEXT         NOT NULL,                         -- ~500 token text chunk
    embedding    vector(768)  NOT NULL,                         -- Gemini text-embedding-004 output
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW()
    -- No updated_at — chunks are write-once during ingestion
);

CREATE INDEX idx_sc_source_id ON source_chunks (source_id);
CREATE INDEX idx_sc_user_id   ON source_chunks (user_id);

-- IVFFlat index for approximate nearest neighbour cosine search
-- NOTE: IVFFlat requires at least 100 rows to be effective.
-- lists=100 means 100 voronoi cells — appropriate for up to ~1M vectors.
-- For production with >1M rows, increase to lists=1000 or use HNSW.
CREATE INDEX idx_sc_embedding ON source_chunks
    USING ivfflat (embedding vector_cosine_ops) WITH (lists = 100);
