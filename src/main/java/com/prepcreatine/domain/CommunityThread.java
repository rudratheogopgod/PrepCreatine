package com.prepcreatine.domain;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * CommunityThread entity — maps to `community_threads` table.
 * ai_summary is generated asynchronously when answerCount > 5.
 */
@Entity
@Table(name = "community_threads")
public class CommunityThread {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "exam_id", nullable = false, length = 50)
    private String examId;

    @Column(name = "subject_id", length = 100)
    private String subjectId;

    @Column(name = "topic_id", length = 200)
    private String topicId;

    @Column(name = "title", nullable = false, length = 300)
    private String title;

    @Column(name = "body", columnDefinition = "TEXT")
    private String body;

    @Column(name = "upvote_count", nullable = false)
    private int upvoteCount = 0;

    @Column(name = "answer_count", nullable = false)
    private int answerCount = 0;

    @Column(name = "is_resolved", nullable = false)
    private boolean isResolved = false;

    @Column(name = "ai_summary", columnDefinition = "TEXT")
    private String aiSummary; // generated async when answerCount > 5

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt = OffsetDateTime.now();

    @PreUpdate
    protected void onUpdate() { this.updatedAt = OffsetDateTime.now(); }

    public CommunityThread() {}

    public UUID getId() { return id; }
    /** authorId is an alias for the DB column user_id (thread author). */
    public UUID getAuthorId() { return userId; }
    public void setAuthorId(UUID authorId) { this.userId = authorId; }
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public String getExamId() { return examId; }
    public void setExamId(String examId) { this.examId = examId; }
    public String getSubjectId() { return subjectId; }
    public void setSubjectId(String subjectId) { this.subjectId = subjectId; }
    public String getTopicId() { return topicId; }
    public void setTopicId(String topicId) { this.topicId = topicId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }
    public int getUpvoteCount() { return upvoteCount; }
    public void setUpvoteCount(int upvoteCount) { this.upvoteCount = upvoteCount; }
    public int getAnswerCount() { return answerCount; }
    public void setAnswerCount(int answerCount) { this.answerCount = answerCount; }
    public boolean isResolved() { return isResolved; }
    public void setResolved(boolean resolved) { isResolved = resolved; }
    public String getAiSummary() { return aiSummary; }
    public void setAiSummary(String aiSummary) { this.aiSummary = aiSummary; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
}
