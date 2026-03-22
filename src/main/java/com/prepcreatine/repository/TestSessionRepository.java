package com.prepcreatine.repository;

import com.prepcreatine.domain.TestSession;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TestSessionRepository extends JpaRepository<TestSession, UUID> {

    long countByUserIdAndStatus(UUID userId, String status);

    @Query("SELECT AVG(t.score) FROM TestSession t WHERE t.userId = :userId AND t.status = 'COMPLETED'")
    Double findAvgScoreForUser(UUID userId);

    List<TestSession> findByUserIdOrderByCreatedAtDesc(UUID userId);

    Optional<TestSession> findByIdAndUserId(UUID id, UUID userId);

    @Query("SELECT t FROM TestSession t WHERE t.userId = :userId AND t.status = 'COMPLETED' ORDER BY t.submittedAt DESC")
    List<TestSession> findCompletedByUserIdOrderByDateInternal(UUID userId, org.springframework.data.domain.Pageable pageable);

    default List<TestSession> findCompletedByUserIdOrderByDate(UUID userId, int limit) {
        return findCompletedByUserIdOrderByDateInternal(userId, PageRequest.of(0, limit));
    }

    /**
     * Count distinct calendar days the user has completed study sessions in the last 14 days.
     * Used by LearnerProfileService to compute consistency_score.
     */
    @Query(value = """
        SELECT COUNT(DISTINCT DATE(created_at AT TIME ZONE 'UTC'))
        FROM test_sessions
        WHERE user_id = :userId
          AND created_at >= NOW() - INTERVAL '14 days'
        """, nativeQuery = true)
    long countDistinctStudyDatesByUserIdInLast14Days(@Param("userId") UUID userId);
}

