-- V8: Create mentor tables

-- ── Mentor-Student Links ──────────────────────────────────────────────────────
CREATE TABLE mentor_student_links (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    mentor_id   UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    student_id  UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    mentor_code VARCHAR(20) NOT NULL,
    linked_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_mentor_student UNIQUE (mentor_id, student_id)
);

CREATE INDEX idx_msl_mentor_id  ON mentor_student_links (mentor_id);
CREATE INDEX idx_msl_student_id ON mentor_student_links (student_id);
CREATE INDEX idx_msl_code       ON mentor_student_links (mentor_code);

-- ── Mentor Notes (one note per student per mentor) ───────────────────────────
CREATE TABLE mentor_notes (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    mentor_id   UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    student_id  UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    content     TEXT        NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TRIGGER trg_mentor_notes_updated_at
    BEFORE UPDATE ON mentor_notes
    FOR EACH ROW EXECUTE FUNCTION update_timestamp();

CREATE INDEX idx_mentor_notes_student ON mentor_notes (mentor_id, student_id);
