package com.prepcreatine.domain;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * UserTopicProgress entity — maps to `user_topic_progress` table.
 * UNIQUE constraint on (user_id, topic_id) enables safe UPSERT operations.
 */
@Entity
@Table(name = "user_topic_progress",
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "topic_id"}))
public class UserTopicProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "topic_id", nullable = false, length = 200)
    private String topicId;

    @Column(name = "exam_id", nullable = false, length = 50)
    private String examId;

    @Column(name = "subject_id", nullable = false, length = 100)
    private String subjectId;

    @Column(name = "status", nullable = false, length = 20)
    private String status = "not_started";

    @Column(name = "time_spent_mins", nullable = false)
    private int timeSpentMins = 0;

    @Column(name = "last_touched")
    private OffsetDateTime lastTouched;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "last_score", precision = 5, scale = 2)
    private java.math.BigDecimal lastScore;

    @Column(name = "test_attempts", nullable = false)
    private int testAttempts = 0;

    // ── SM-2 Spaced Repetition fields ─────────────────────────────────────────
    @Column(name = "easiness_factor", nullable = false, precision = 4, scale = 2)
    private java.math.BigDecimal easinessFactor = new java.math.BigDecimal("2.5");

    @Column(name = "repetition_count", nullable = false)
    private int repetitionCount = 0;

    @Column(name = "interval_days", nullable = false)
    private int intervalDays = 1;

    @Column(name = "next_review_date")
    private java.time.LocalDate nextReviewDate;

    @Column(name = "last_reviewed_at")
    private OffsetDateTime lastReviewedAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt = OffsetDateTime.now();

    @PreUpdate
    protected void onUpdate() { this.updatedAt = OffsetDateTime.now(); }

    public UserTopicProgress() {}


    // ── Getters & Setters ─────────────────────────────────────────────────────

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public String getTopicId() { return topicId; }
    public void setTopicId(String topicId) { this.topicId = topicId; }
    public String getExamId() { return examId; }
    public void setExamId(String examId) { this.examId = examId; }
    public String getSubjectId() { return subjectId; }
    public void setSubjectId(String subjectId) { this.subjectId = subjectId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public int getTimeSpentMins() { return timeSpentMins; }
    public void setTimeSpentMins(int timeSpentMins) { this.timeSpentMins = timeSpentMins; }
    public java.math.BigDecimal getLastScore() { return lastScore; }
    public void setLastScore(java.math.BigDecimal lastScore) { this.lastScore = lastScore; }
    public int getTestAttempts() { return testAttempts; }
    public void setTestAttempts(int testAttempts) { this.testAttempts = testAttempts; }
    public OffsetDateTime getLastTouched() { return lastTouched; }
    public void setLastTouched(OffsetDateTime lastTouched) { this.lastTouched = lastTouched; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }

    // ── SM-2 getters & setters ───────────────────────────────────────────────
    public java.math.BigDecimal getEasinessFactor() { return easinessFactor; }
    public void setEasinessFactor(java.math.BigDecimal easinessFactor) { this.easinessFactor = easinessFactor; }
    public int getRepetitionCount() { return repetitionCount; }
    public void setRepetitionCount(int repetitionCount) { this.repetitionCount = repetitionCount; }
    public int getIntervalDays() { return intervalDays; }
    public void setIntervalDays(int intervalDays) { this.intervalDays = intervalDays; }
    public java.time.LocalDate getNextReviewDate() { return nextReviewDate; }
    public void setNextReviewDate(java.time.LocalDate nextReviewDate) { this.nextReviewDate = nextReviewDate; }
    public OffsetDateTime getLastReviewedAt() { return lastReviewedAt; }
    public void setLastReviewedAt(OffsetDateTime lastReviewedAt) { this.lastReviewedAt = lastReviewedAt; }
}

