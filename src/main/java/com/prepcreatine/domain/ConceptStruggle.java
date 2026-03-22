package com.prepcreatine.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/**
 * Concept-level struggle tracking.
 * Incremented when a student gets a question wrong that touches this concept tag.
 * Used by agentic RAG to enriche retrieval queries with the student's weak areas.
 */
@Entity
@Table(name = "concept_struggles",
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "concept_tag"}))
public class ConceptStruggle {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "topic_id", nullable = false, length = 200)
    private String topicId;

    @Column(name = "concept_tag", nullable = false, length = 200)
    private String conceptTag;  // e.g. "rate_of_change", "electron_movement"

    @Column(name = "struggle_count", nullable = false)
    private Integer struggleCount = 1;

    @Column(name = "last_seen_at", nullable = false)
    private Instant lastSeenAt = Instant.now();

    public ConceptStruggle() {}

    // ── Getters & Setters ──────────────────────────────────────────────────

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public String getTopicId() { return topicId; }
    public void setTopicId(String topicId) { this.topicId = topicId; }
    public String getConceptTag() { return conceptTag; }
    public void setConceptTag(String conceptTag) { this.conceptTag = conceptTag; }
    public Integer getStruggleCount() { return struggleCount != null ? struggleCount : 0; }
    public void setStruggleCount(Integer struggleCount) { this.struggleCount = struggleCount; }
    public Instant getLastSeenAt() { return lastSeenAt; }
    public void setLastSeenAt(Instant lastSeenAt) { this.lastSeenAt = lastSeenAt; }
}
