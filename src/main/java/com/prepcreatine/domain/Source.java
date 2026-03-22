package com.prepcreatine.domain;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Source entity — maps to `sources` table.
 * study_guide JSONB stores AI-generated summary, topics, formulas, exam questions.
 */
@Entity
@Table(name = "sources")
public class Source {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "type", nullable = false, length = 20)
    private String type; // 'url', 'pdf', 'text'

    @Column(name = "title", nullable = false, length = 500)
    private String title;

    @Column(name = "original_url", length = 2048)
    private String originalUrl;

    @Column(name = "raw_text", columnDefinition = "TEXT")
    private String rawText;

    @Column(name = "status", nullable = false, length = 20)
    private String status = "pending"; // 'pending','processing','ready','failed'

    @Column(name = "error_message", length = 500)
    private String errorMessage;

    @Column(name = "topic_count", nullable = false)
    private int topicCount = 0;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "study_guide", columnDefinition = "jsonb")
    private JsonNode studyGuide;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt = OffsetDateTime.now();

    @PreUpdate
    protected void onUpdate() { this.updatedAt = OffsetDateTime.now(); }

    public Source() {}

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getOriginalUrl() { return originalUrl; }
    public void setOriginalUrl(String originalUrl) { this.originalUrl = originalUrl; }
    public String getUrl() { return originalUrl; }
    public void setUrl(String url) { this.originalUrl = url; }
    public String getRawText() { return rawText; }
    public void setRawText(String rawText) { this.rawText = rawText; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public int getTopicCount() { return topicCount; }
    public void setTopicCount(int topicCount) { this.topicCount = topicCount; }
    public JsonNode getStudyGuide() { return studyGuide; }
    public void setStudyGuide(JsonNode studyGuide) { this.studyGuide = studyGuide; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
}
