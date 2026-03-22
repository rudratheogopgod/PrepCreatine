package com.prepcreatine.repository;

import com.prepcreatine.domain.SourceChunk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SourceChunkRepository extends JpaRepository<SourceChunk, UUID> {
    
    void deleteBySourceId(UUID sourceId);
    
    // Note: uses pgvector syntax `<->` for cosine distance. The operator expects vector cast.
    @Query(value = "SELECT * FROM source_chunks c WHERE c.source_id = :sourceId " +
                   "ORDER BY c.embedding <-> cast(:embedding as vector) ASC LIMIT :k", 
           nativeQuery = true)
    List<SourceChunk> findTopKByCosineSimilarity(UUID sourceId, float[] embedding, int k);
}
