-- V10: Enable pgvector extension and other required extensions
-- NOTE: Requires superuser or pg_extension_owner privilege.
-- On Railway, Supabase, and Neon, pgvector is pre-installed.

CREATE EXTENSION IF NOT EXISTS vector;        -- pgvector: vector similarity search
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";   -- uuid_generate_v4() (gen_random_uuid preferred)
CREATE EXTENSION IF NOT EXISTS pg_trgm;       -- trigram text search (future full-text search)
