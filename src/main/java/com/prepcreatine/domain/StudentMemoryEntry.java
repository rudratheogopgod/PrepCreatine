package com.prepcreatine.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/**
 * Cross-session persistent memory entry.
 * Records what the AI has explained, misconceptions corrected, and difficulty signals.
 * Injected into every chat prompt so the AI remembers past sessions.
 *
 * memory_type values:
 *   'concept_explained'  — AI explained a concept
 *   'misconception'      — student had a misconception, was corrected
 *   'preferred_example'  — student responded well to a specific example type
 *   'difficulty_signal'  — student expressed confusion about a topic
 */
@Entity
@Table(name = "student_memory_entries")
public class StudentMemoryEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "memory_type", nullable = false, length = 50)
    private String memoryType;

    @Column(name = "topic_id", length = 200)
    private String topicId;

    @Column(name = "concept", length = 300)
    private String concept;

    @Column(name = "summary", nullable = false, columnDefinition = "TEXT")
    private String summary;

    @Column(name = "importance", nullable = false)
    private Short importance = 1;  // 1=low, 2=medium, 3=high

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "expires_at")
    private Instant expiresAt;  // NULL = permanent

    public StudentMemoryEntry() {}

    // ── Getters & Setters ──────────────────────────────────────────────────

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public String getMemoryType() { return memoryType; }
    public void setMemoryType(String memoryType) { this.memoryType = memoryType; }
    public String getTopicId() { return topicId; }
    public void setTopicId(String topicId) { this.topicId = topicId; }
    public String getConcept() { return concept; }
    public void setConcept(String concept) { this.concept = concept; }
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    public Short getImportance() { return importance; }
    public void setImportance(Short importance) { this.importance = importance; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }
}
