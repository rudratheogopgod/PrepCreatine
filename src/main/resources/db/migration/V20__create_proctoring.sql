-- V20: Proctoring events table
-- Stores gaze-away, tab-switch, and face-lost events per test session.
-- Used by ProctorController + ProctorService for integrity reporting.

CREATE TABLE IF NOT EXISTS proctoring_events (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    test_session_id UUID NOT NULL REFERENCES test_sessions(id) ON DELETE CASCADE,
    event_type      VARCHAR(50) NOT NULL
                    CHECK (event_type IN ('gaze_away','tab_switch','face_lost')),
    event_count     INTEGER NOT NULL DEFAULT 1,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT proctoring_events_session_type_uq UNIQUE (test_session_id, event_type)
);

CREATE INDEX IF NOT EXISTS idx_proctor_session ON proctoring_events(test_session_id);
CREATE INDEX IF NOT EXISTS idx_proctor_user    ON proctoring_events(user_id);
