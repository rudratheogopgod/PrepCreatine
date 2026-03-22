-- Enable pgvector extension (required for RAG embeddings)
CREATE EXTENSION IF NOT EXISTS vector;

-- Verify it loaded
SELECT extname, extversion FROM pg_extension WHERE extname = 'vector';
