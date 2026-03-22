package com.prepcreatine.repository;

import com.prepcreatine.domain.SystemSourceChunk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SystemSourceChunkRepository extends JpaRepository<SystemSourceChunk, UUID> {

    List<SystemSourceChunk> findBySourceId(UUID sourceId);

    /**
     * Vector similarity search within the system corpus for a given exam type.
     * Returns top K chunks by cosine similarity to the query embedding.
     * Note: For zero-vector embeddings (hackathon placeholder), this returns
     * rows in insertion order — still useful for demo since content is relevant.
     */
    @Query(value = """
        SELECT * FROM system_source_chunks
        WHERE exam_id = :examId
        ORDER BY embedding <=> CAST(:embedding AS vector)
        LIMIT :topK
        """, nativeQuery = true)
    List<SystemSourceChunk> findTopKByExamId(
        @Param("examId") String examId,
        @Param("embedding") float[] embedding,
        @Param("topK") int topK);

    /**
     * For corpus seeding — check if chunks exist for a given source.
     */
    long countBySourceId(UUID sourceId);

    /**
     * Total system chunk count (used to detect if seeding is needed).
     */
    long count();
}
