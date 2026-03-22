package com.prepcreatine.domain;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Message entity — maps to `messages` table.
 * concept_map JSONB: AI-generated React Flow graph (nodes + edges).
 */
@Entity
@Table(name = "messages")
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "conversation_id", nullable = false)
    private UUID conversationId;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "role", nullable = false, length = 10)
    private String role; // 'user' or 'assistant'

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "concept_map", columnDefinition = "jsonb")
    private JsonNode conceptMap;

    @Column(name = "youtube_ids", columnDefinition = "text[]")
    private String[] youtubeIds = new String[]{};

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    public Message() {}

    public UUID getId() { return id; }
    public UUID getConversationId() { return conversationId; }
    public void setConversationId(UUID conversationId) { this.conversationId = conversationId; }
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public JsonNode getConceptMap() { return conceptMap; }
    public void setConceptMap(JsonNode conceptMap) { this.conceptMap = conceptMap; }
    public String[] getYoutubeIds() { return youtubeIds; }
    public void setYoutubeIds(String[] youtubeIds) { this.youtubeIds = youtubeIds; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
}
