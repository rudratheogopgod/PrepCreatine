package com.prepcreatine.repository;

import com.prepcreatine.domain.StudentMemoryEntry;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for student_memory_entries table.
 * Memories decay via expires_at; non-expired entries are injected into chat prompts.
 */
@Repository
public interface StudentMemoryRepository extends JpaRepository<StudentMemoryEntry, UUID> {

    /**
     * Recent non-expired memories, ordered by importance DESC then newest first.
     * Used to build the "STUDENT MEMORY" block injected into chat system prompts.
     */
    @Query("""
        SELECT m FROM StudentMemoryEntry m
        WHERE m.userId = :userId
          AND (m.expiresAt IS NULL OR m.expiresAt > CURRENT_TIMESTAMP)
        ORDER BY m.importance DESC, m.createdAt DESC
        """)
    List<StudentMemoryEntry> findByUserIdAndNotExpired(
        @Param("userId") UUID userId, Pageable pageable);

    /** Convenience overload accepting a plain int limit. */
    default List<StudentMemoryEntry> findByUserIdAndNotExpired(UUID userId, int limit) {
        return findByUserIdAndNotExpired(userId, PageRequest.of(0, limit));
    }

    List<StudentMemoryEntry> findByUserId(UUID userId);
}
