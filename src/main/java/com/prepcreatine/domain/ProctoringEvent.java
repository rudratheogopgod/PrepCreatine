package com.prepcreatine.domain;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * ProctoringEvent — records gaze-away, tab-switch, face-lost events per test session.
 * Unique constraint on (test_session_id, event_type) enables upsert via native query.
 * Maps to `proctoring_events` table (created in V20 migration).
 */
@Entity
@Table(name = "proctoring_events",
       uniqueConstraints = @UniqueConstraint(
           name = "proctoring_events_session_type_uq",
           columnNames = {"test_session_id", "event_type"}))
public class ProctoringEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "test_session_id", nullable = false)
    private UUID testSessionId;

    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType;   // "gaze_away" | "tab_switch" | "face_lost"

    @Column(name = "event_count", nullable = false)
    private int eventCount = 1;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    public ProctoringEvent() {}

    // ── Getters & Setters ───────────────────────────────────────────────────
    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public UUID getTestSessionId() { return testSessionId; }
    public void setTestSessionId(UUID testSessionId) { this.testSessionId = testSessionId; }
    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }
    public int getEventCount() { return eventCount; }
    public void setEventCount(int eventCount) { this.eventCount = eventCount; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
}
