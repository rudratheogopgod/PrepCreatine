package com.prepcreatine.repository;

import com.prepcreatine.domain.TestSession;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
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
}
