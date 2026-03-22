-- V5: Create conversations and messages tables

-- ── Conversations (chat sessions) ────────────────────────────────────────────
CREATE TABLE conversations (
    id              UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title           VARCHAR(200) NOT NULL DEFAULT 'New conversation',
    exam_id         VARCHAR(50),
    source_id       UUID REFERENCES sources(id) ON DELETE SET NULL,  -- source-scoped RAG
    message_count   INTEGER      NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE TRIGGER trg_conversations_updated_at
    BEFORE UPDATE ON conversations
    FOR EACH ROW EXECUTE FUNCTION update_timestamp();

CREATE INDEX idx_conversations_user_id ON conversations (user_id, created_at DESC);

-- ── Messages (individual chat turns) ─────────────────────────────────────────
CREATE TABLE messages (
    id               UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    conversation_id  UUID        NOT NULL REFERENCES conversations(id) ON DELETE CASCADE,
    user_id          UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role             VARCHAR(10) NOT NULL CHECK (role IN ('user','assistant')),
    content          TEXT        NOT NULL,
    concept_map      JSONB,                                     -- AI concept graph JSON
    youtube_ids      TEXT[]      NOT NULL DEFAULT '{}',         -- cached video IDs
    created_at       TIMESTAMPTZ NOT NULL DEFAULT NOW()
    -- No updated_at — messages are immutable once created
);

CREATE INDEX idx_messages_conversation ON messages (conversation_id, created_at ASC);
