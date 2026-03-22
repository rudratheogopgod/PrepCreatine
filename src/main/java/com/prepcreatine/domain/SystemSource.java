package com.prepcreatine.domain;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * SystemSource — stores NCERT / system-level knowledge corpus entries.
 * Shared across all users — NOT per-user. Seeded by SystemCorpusSeeder on startup.
 * Maps to `system_sources` table (created in V19 migration).
 */
@Entity
@Table(name = "system_sources")
public class SystemSource {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "exam_id", nullable = false, length = 50)
    private String examId;

    @Column(name = "subject_id", nullable = false, length = 100)
    private String subjectId;

    @Column(name = "title", nullable = false, length = 500)
    private String title;

    @Column(name = "raw_text", nullable = false, columnDefinition = "TEXT")
    private String rawText;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    public SystemSource() {}

    // ── Getters & Setters ───────────────────────────────────────────────────
    public UUID getId() { return id; }
    public String getExamId() { return examId; }
    public void setExamId(String examId) { this.examId = examId; }
    public String getSubjectId() { return subjectId; }
    public void setSubjectId(String subjectId) { this.subjectId = subjectId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getRawText() { return rawText; }
    public void setRawText(String rawText) { this.rawText = rawText; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
}
