-- V23: Student Memory Entries — cross-session persistent memory
-- Stores what the AI has explained, misconceptions corrected, difficulty signals
-- Implements Component E: Persistent Conversation Memory

CREATE TABLE IF NOT EXISTS student_memory_entries (
    id           UUID         DEFAULT gen_random_uuid() PRIMARY KEY,
    user_id      UUID         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    memory_type  VARCHAR(50)  NOT NULL,
      -- 'concept_explained'  — AI explained a concept
      -- 'misconception'      — student had a misconception, was corrected
      -- 'preferred_example'  — student responded well to a specific example type
      -- 'difficulty_signal'  — student indicated difficulty with a topic
    topic_id     VARCHAR(200),
    concept      VARCHAR(300),
    summary      TEXT         NOT NULL,  -- one sentence describing what happened
    importance   SMALLINT     NOT NULL DEFAULT 1,  -- 1=low, 2=medium, 3=high
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    expires_at   TIMESTAMPTZ            -- NULL = permanent; otherwise memory decays
);

CREATE INDEX IF NOT EXISTS idx_memory_user
    ON student_memory_entries (user_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_memory_topic
    ON student_memory_entries (user_id, topic_id);

CREATE INDEX IF NOT EXISTS idx_memory_expires
    ON student_memory_entries (expires_at)
    WHERE expires_at IS NOT NULL;
