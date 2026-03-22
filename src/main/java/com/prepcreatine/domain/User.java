package com.prepcreatine.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * User entity — maps to the `users` table.
 * Role: STUDENT (default), MENTOR, ADMIN.
 * is_active: false = soft-deleted.
 * share_token: unique short token for public progress share link.
 */
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "email", nullable = false, unique = true, length = 254)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 128)
    private String passwordHash;

    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    @Column(name = "role", nullable = false, length = 20)
    private String role = "STUDENT";

    @Column(name = "is_email_verified", nullable = false)
    private boolean isEmailVerified = false;

    @Column(name = "onboarding_complete", nullable = false)
    private boolean onboardingComplete = false;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;

    @Column(name = "share_token", unique = true, length = 64)
    private String shareToken;

    // ── Exam & Study Preferences ───────────────────────────────────────────────

    @Column(name = "exam_type", length = 50)
    private String examType;

    @Column(name = "exam_date")
    private LocalDate examDate;

    /** study_mode: exam_prep | topic_deep_dive | speed_run */
    @Column(name = "study_mode", length = 30)
    private String studyMode;

    @Column(name = "daily_goal_mins")
    private Integer dailyGoalMins = 60;

    // ── Streak & Activity ──────────────────────────────────────────────────────

    @Column(name = "current_streak", nullable = false)
    private int currentStreak = 0;

    @Column(name = "longest_streak", nullable = false)
    private int longestStreak = 0;

    @Column(name = "total_days", nullable = false)
    private int totalDays = 0;

    // ── Readiness Score ────────────────────────────────────────────────────────

    /** 0–100 computed readiness score per BSDD §12. */
    @Column(name = "readiness_score", nullable = false)
    private int readinessScore = 0;

    // ── Mentor ─────────────────────────────────────────────────────────────────

    /** 6-character alphanumeric code for mentor invite. Only set for MENTOR role. */
    @Column(name = "mentor_code", unique = true, length = 20)
    private String mentorCode;

    // ── Timestamps ─────────────────────────────────────────────────────────────

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt = OffsetDateTime.now();

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }

    // ── Constructors ──────────────────────────────────────────────────────────

    public User() {}

    // ── Getters & Setters ─────────────────────────────────────────────────────

    public UUID getId() { return id; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public boolean isEmailVerified() { return isEmailVerified; }
    public void setEmailVerified(boolean emailVerified) { isEmailVerified = emailVerified; }

    public boolean isOnboardingComplete() { return onboardingComplete; }
    public void setOnboardingComplete(boolean onboardingComplete) { this.onboardingComplete = onboardingComplete; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }

    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }

    public String getShareToken() { return shareToken; }
    public void setShareToken(String shareToken) { this.shareToken = shareToken; }

    public String getExamType() { return examType; }
    public void setExamType(String examType) { this.examType = examType; }

    public LocalDate getExamDate() { return examDate; }
    public void setExamDate(LocalDate examDate) { this.examDate = examDate; }

    public String getStudyMode() { return studyMode; }
    public void setStudyMode(String studyMode) { this.studyMode = studyMode; }

    public Integer getDailyGoalMins() { return dailyGoalMins; }
    public void setDailyGoalMins(Integer dailyGoalMins) { this.dailyGoalMins = dailyGoalMins; }

    public int getCurrentStreak() { return currentStreak; }
    public void setCurrentStreak(int currentStreak) { this.currentStreak = currentStreak; }

    public int getLongestStreak() { return longestStreak; }
    public void setLongestStreak(int longestStreak) { this.longestStreak = longestStreak; }

    public int getTotalDays() { return totalDays; }
    public void setTotalDays(int totalDays) { this.totalDays = totalDays; }

    public int getReadinessScore() { return readinessScore; }
    public void setReadinessScore(int readinessScore) { this.readinessScore = readinessScore; }

    public String getMentorCode() { return mentorCode; }
    public void setMentorCode(String mentorCode) { this.mentorCode = mentorCode; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
}
