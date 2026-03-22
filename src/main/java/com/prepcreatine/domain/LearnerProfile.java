package com.prepcreatine.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Persistent behavioral model of a student — updated after every test and study session.
 * Written by LearnerProfileService; read by LearnerAnalysisAgent and all AI prompts.
 */
@Entity
@Table(name = "learner_profiles")
public class LearnerProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false, unique = true)
    private UUID userId;

    @Column(name = "avg_time_per_correct", precision = 6, scale = 2)
    private BigDecimal avgTimePerCorrect = BigDecimal.ZERO;

    @Column(name = "avg_time_per_wrong", precision = 6, scale = 2)
    private BigDecimal avgTimePerWrong = BigDecimal.ZERO;

    @Column(name = "struggle_indicator", precision = 4, scale = 3)
    private BigDecimal struggleIndicator = BigDecimal.ZERO;

    @Column(name = "consistency_score", precision = 4, scale = 3)
    private BigDecimal consistencyScore = BigDecimal.ZERO;

    // AI-generated insight fields (written by LearnerAnalysisAgent)
    @Column(name = "weakness_pattern", columnDefinition = "TEXT")
    private String weaknessPattern;

    @Column(name = "strength_pattern", columnDefinition = "TEXT")
    private String strengthPattern;

    @Column(name = "recommended_mode", length = 30)
    private String recommendedMode;

    @Column(name = "learning_velocity", precision = 4, scale = 3)
    private BigDecimal learningVelocity = BigDecimal.valueOf(0.5);

    @Column(name = "last_analyzed_at")
    private Instant lastAnalyzedAt;

    @Column(name = "total_study_sessions")
    private Integer totalStudySessions = 0;

    @Column(name = "total_questions_seen")
    private Integer totalQuestionsSeen = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt = OffsetDateTime.now();

    @PreUpdate
    protected void onUpdate() { this.updatedAt = OffsetDateTime.now(); }

    public LearnerProfile() {}

    public LearnerProfile(UUID userId) {
        this.userId = userId;
    }

    // ── Getters & Setters ──────────────────────────────────────────────────

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public BigDecimal getAvgTimePerCorrect() { return avgTimePerCorrect; }
    public void setAvgTimePerCorrect(BigDecimal v) { this.avgTimePerCorrect = v; }

    public BigDecimal getAvgTimePerWrong() { return avgTimePerWrong; }
    public void setAvgTimePerWrong(BigDecimal v) { this.avgTimePerWrong = v; }

    public BigDecimal getStruggleIndicator() { return struggleIndicator; }
    public void setStruggleIndicator(BigDecimal v) { this.struggleIndicator = v; }

    public BigDecimal getConsistencyScore() { return consistencyScore; }
    public void setConsistencyScore(BigDecimal v) { this.consistencyScore = v; }

    public String getWeaknessPattern() { return weaknessPattern; }
    public void setWeaknessPattern(String v) { this.weaknessPattern = v; }

    public String getStrengthPattern() { return strengthPattern; }
    public void setStrengthPattern(String v) { this.strengthPattern = v; }

    public String getRecommendedMode() { return recommendedMode; }
    public void setRecommendedMode(String v) { this.recommendedMode = v; }

    public BigDecimal getLearningVelocity() { return learningVelocity; }
    public void setLearningVelocity(BigDecimal v) { this.learningVelocity = v; }

    public Instant getLastAnalyzedAt() { return lastAnalyzedAt; }
    public void setLastAnalyzedAt(Instant v) { this.lastAnalyzedAt = v; }

    public Integer getTotalStudySessions() { return totalStudySessions != null ? totalStudySessions : 0; }
    public void setTotalStudySessions(Integer v) { this.totalStudySessions = v; }

    public Integer getTotalQuestionsSeen() { return totalQuestionsSeen != null ? totalQuestionsSeen : 0; }
    public void setTotalQuestionsSeen(Integer v) { this.totalQuestionsSeen = v; }

    /** Convenience: increment totalQuestionsSeen safely */
    public void setTotalQuestionsSeenOrDefault(int count) {
        this.totalQuestionsSeen = count;
    }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
}
