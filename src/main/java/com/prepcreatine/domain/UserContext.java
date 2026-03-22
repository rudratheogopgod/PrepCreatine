package com.prepcreatine.domain;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * UserContext entity — maps to `user_contexts` table.
 * Stores AI personalization context per user.
 * weak_topics / strong_topics: stored as native PostgreSQL text[] arrays.
 */
@Entity
@Table(name = "user_contexts")
public class UserContext {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false, unique = true)
    private UUID userId;

    @Column(name = "exam_type", length = 50)
    private String examType;

    @Column(name = "exam_date")
    private LocalDate examDate;

    @Column(name = "study_mode", nullable = false, length = 30)
    private String studyMode = "in_depth";

    @Column(name = "daily_goal_mins", nullable = false)
    private int dailyGoalMins = 90;

    /** Array of weak topic IDs (PostgreSQL text[]). */
    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "weak_topics", columnDefinition = "text[]")
    private String[] weakTopics = new String[]{};

    /** Array of strong topic IDs (PostgreSQL text[]). */
    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "strong_topics", columnDefinition = "text[]")
    private String[] strongTopics = new String[]{};

    @Column(name = "custom_topic", length = 200)
    private String customTopic;

    @Column(name = "theme", nullable = false, length = 10)
    private String theme = "system";

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt = OffsetDateTime.now();

    @PreUpdate
    protected void onUpdate() { this.updatedAt = OffsetDateTime.now(); }

    public UserContext() {}

    // ── Getters & Setters ─────────────────────────────────────────────────────

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public String getExamType() { return examType; }
    public void setExamType(String examType) { this.examType = examType; }
    public LocalDate getExamDate() { return examDate; }
    public void setExamDate(LocalDate examDate) { this.examDate = examDate; }
    public String getStudyMode() { return studyMode; }
    public void setStudyMode(String studyMode) { this.studyMode = studyMode; }
    public int getDailyGoalMins() { return dailyGoalMins; }
    public void setDailyGoalMins(int dailyGoalMins) { this.dailyGoalMins = dailyGoalMins; }

    public String[] getWeakTopics() { return weakTopics; }
    public void setWeakTopics(String[] weakTopics) { this.weakTopics = weakTopics; }

    public String[] getStrongTopics() { return strongTopics; }
    public void setStrongTopics(String[] strongTopics) { this.strongTopics = strongTopics; }

    public String getCustomTopic() { return customTopic; }
    public void setCustomTopic(String customTopic) { this.customTopic = customTopic; }
    public String getTheme() { return theme; }
    public void setTheme(String theme) { this.theme = theme; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
}
