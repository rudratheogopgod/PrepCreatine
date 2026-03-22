package com.prepcreatine.repository;

import com.prepcreatine.domain.ConceptStruggle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for concept_struggles table.
 * Provides upsert (INSERT ... ON CONFLICT) and ordered top-K queries.
 */
@Repository
public interface ConceptStruggleRepository extends JpaRepository<ConceptStruggle, UUID> {

    List<ConceptStruggle> findByUserId(UUID userId);

    /**
     * Top-K concepts the student struggles with most, ordered by struggle_count DESC.
     */
    @Query(value = """
        SELECT * FROM concept_struggles
        WHERE user_id = :userId
        ORDER BY struggle_count DESC
        LIMIT :limit
        """, nativeQuery = true)
    List<ConceptStruggle> findTopByUserIdOrderByStruggleCountDesc(
        @Param("userId") UUID userId,
        @Param("limit") int limit);

    /**
     * UPSERT: insert struggle record or increment count if it already exists.
     * Called after every wrong answer is graded.
     */
    @Modifying
    @Query(value = """
        INSERT INTO concept_struggles (id, user_id, topic_id, concept_tag, struggle_count, last_seen_at)
        VALUES (gen_random_uuid(), :userId, :topicId, :conceptTag, 1, NOW())
        ON CONFLICT (user_id, concept_tag)
        DO UPDATE SET
            struggle_count = concept_struggles.struggle_count + 1,
            last_seen_at   = NOW()
        """, nativeQuery = true)
    void upsertStruggle(@Param("userId") UUID userId,
                        @Param("topicId") String topicId,
                        @Param("conceptTag") String conceptTag);

    /**
     * Increment struggle count for a tag the student has asked about in chat
     * (only if the tag already exists in their struggle set).
     */
    @Modifying
    @Query(value = """
        UPDATE concept_struggles
        SET struggle_count = struggle_count + 1, last_seen_at = NOW()
        WHERE user_id = :userId AND concept_tag = :tag
        """, nativeQuery = true)
    void incrementStruggle(@Param("userId") UUID userId,
                           @Param("tag") String tag);
}
