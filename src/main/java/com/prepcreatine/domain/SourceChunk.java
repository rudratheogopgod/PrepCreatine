package com.prepcreatine.domain;

import com.pgvector.PGvector;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * SourceChunk entity — maps to `source_chunks` table.
 * Each chunk is ~500 tokens of source text with a 768-dimensional embedding.
 * The embedding vector is used for pgvector cosine similarity search (RAG).
 *
 * [PERF] The IVFFlat index on the embedding column accelerates similarity search.
 * [NOTE] pgvector requires the `com.pgvector:pgvector:0.1.6` dependency.
 */
@Entity
@Table(name = "source_chunks")
public class SourceChunk {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "source_id", nullable = false)
    private UUID sourceId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "chunk_index", nullable = false)
    private int chunkIndex;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    /**
     * 768-dimensional embedding from Gemini text-embedding-004.
     * Stored as PostgreSQL vector(768) type.
     * Hibernate maps this using the pgvector library's float[] → vector mapping.
     */
    @Column(name = "embedding", nullable = false, columnDefinition = "vector(768)")
    private float[] embedding;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    public SourceChunk() {}

    public UUID getId() { return id; }
    public UUID getSourceId() { return sourceId; }
    public void setSourceId(UUID sourceId) { this.sourceId = sourceId; }
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public int getChunkIndex() { return chunkIndex; }
    public void setChunkIndex(int chunkIndex) { this.chunkIndex = chunkIndex; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public float[] getEmbedding() { return embedding; }
    public void setEmbedding(float[] embedding) { this.embedding = embedding; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
}
