package com.prepcreatine.domain;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Session entity — maps to `sessions` table.
 * UNIQUE constraint on (user_id, date) enables UPSERT in SessionLoggingService.
 * One row per user per calendar day — updated incrementally.
 */
@Entity
@Table(name = "sessions",
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "date"}))
public class Session {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "date", nullable = false)
    private LocalDate date;

    @Column(name = "duration_mins", nullable = false)
    private int durationMins = 0;

    @Column(name = "mode_used", length = 30)
    private String modeUsed;

    @Column(name = "topics_touched", columnDefinition = "text[]")
    private String[] topicsTouched = new String[]{};

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt = OffsetDateTime.now();

    @PreUpdate
    protected void onUpdate() { this.updatedAt = OffsetDateTime.now(); }

    public Session() {}

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }
    public int getDurationMins() { return durationMins; }
    public void setDurationMins(int durationMins) { this.durationMins = durationMins; }
    public String getModeUsed() { return modeUsed; }
    public void setModeUsed(String modeUsed) { this.modeUsed = modeUsed; }
    public String[] getTopicsTouched() { return topicsTouched; }
    public void setTopicsTouched(String[] topicsTouched) { this.topicsTouched = topicsTouched; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
}
