package com.prepcreatine.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * TestSession entity — maps to `test_sessions` table.
 * Represents a single mock test attempt.
 * correct_answer is NOT returned in API responses until after submission.
 */
@Entity
@Table(name = "test_sessions")
public class TestSession {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "exam_id", nullable = false, length = 50)
    private String examId;

    @Column(name = "test_type", nullable = false, length = 20)
    private String testType; // 'full_mock', 'topic_wise', 'rapid_fire'

    @Column(name = "subject_id", length = 100)
    private String subjectId;

    @Column(name = "topic_id", length = 200)
    private String topicId;

    @Column(name = "level")
    private Short level; // 1, 2, or 3

    @Column(name = "status", nullable = false, length = 20)
    private String status = "in_progress"; // 'in_progress', 'submitted', 'abandoned'

    @Column(name = "total_questions", nullable = false)
    private int totalQuestions = 0;

    @Column(name = "answered_count", nullable = false)
    private int answeredCount = 0;

    @Column(name = "correct_count", nullable = false)
    private int correctCount = 0;

    @Column(name = "score", precision = 5, scale = 2)
    private BigDecimal score; // percentage 0.00-100.00

    @Column(name = "time_taken_secs")
    private Integer timeTakenSecs;
    
    @Column(name = "time_limit_mins")
    private Integer timeLimitMins;
    
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "question_ids", columnDefinition = "jsonb")
    private List<UUID> questionIds;

    @Column(name = "started_at", nullable = false, updatable = false)
    private OffsetDateTime startedAt = OffsetDateTime.now();

    @Column(name = "submitted_at")
    private OffsetDateTime submittedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt = OffsetDateTime.now();

    @PreUpdate
    protected void onUpdate() { this.updatedAt = OffsetDateTime.now(); }

    public TestSession() {}

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public String getExamId() { return examId; }
    public void setExamId(String examId) { this.examId = examId; }
    public String getTestType() { return testType; }
    public void setTestType(String testType) { this.testType = testType; }
    public String getSubjectId() { return subjectId; }
    public void setSubjectId(String subjectId) { this.subjectId = subjectId; }
    public String getTopicId() { return topicId; }
    public void setTopicId(String topicId) { this.topicId = topicId; }
    public Short getLevel() { return level; }
    public void setLevel(Short level) { this.level = level; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public int getTotalQuestions() { return totalQuestions; }
    public void setTotalQuestions(int totalQuestions) { this.totalQuestions = totalQuestions; }
    public int getAnsweredCount() { return answeredCount; }
    public void setAnsweredCount(int answeredCount) { this.answeredCount = answeredCount; }
    public int getCorrectCount() { return correctCount; }
    public void setCorrectCount(int correctCount) { this.correctCount = correctCount; }
    public BigDecimal getScore() { return score; }
    public void setScore(BigDecimal score) { this.score = score; }
    public Integer getTimeTakenSecs() { return timeTakenSecs; }
    public void setTimeTakenSecs(Integer timeTakenSecs) { this.timeTakenSecs = timeTakenSecs; }
    public Integer getTimeLimitMins() { return timeLimitMins; }
    public void setTimeLimitMins(Integer timeLimitMins) { this.timeLimitMins = timeLimitMins; }
    public List<UUID> getQuestionIds() { return questionIds; }
    public void setQuestionIds(List<UUID> questionIds) { this.questionIds = questionIds; }
    public OffsetDateTime getStartedAt() { return startedAt; }
    public OffsetDateTime getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(OffsetDateTime submittedAt) { this.submittedAt = submittedAt; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
}
