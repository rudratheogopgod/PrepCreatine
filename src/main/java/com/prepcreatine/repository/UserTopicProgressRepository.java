package com.prepcreatine.repository;

import com.prepcreatine.domain.UserTopicProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserTopicProgressRepository extends JpaRepository<UserTopicProgress, UUID> {

    List<UserTopicProgress> findByUserId(UUID userId);

    Optional<UserTopicProgress> findByUserIdAndTopicId(UUID userId, String topicId);

    long countByUserId(UUID userId);

    long countByUserIdAndStatus(UUID userId, String status);

    /**
     * Returns topics due for spaced-repetition review (next_review_date <= today).
     * Ordered by most overdue first.
     */
    @Query("""
        SELECT p FROM UserTopicProgress p
        WHERE p.userId = :userId
          AND p.nextReviewDate IS NOT NULL
          AND p.nextReviewDate <= :today
          AND p.status = 'done'
        ORDER BY p.nextReviewDate ASC
        """)
    List<UserTopicProgress> findDueForReview(@Param("userId") UUID userId,
                                              @Param("today") LocalDate today);

    /**
     * Count of topics due for review today or earlier.
     */
    @Query("""
        SELECT COUNT(p) FROM UserTopicProgress p
        WHERE p.userId = :userId
          AND p.nextReviewDate IS NOT NULL
          AND p.nextReviewDate <= :today
          AND p.status = 'done'
        """)
    long countDueForReview(@Param("userId") UUID userId, @Param("today") LocalDate today);

    /**
     * Finds latest done topics for the planner (stale reviews to recommend).
     */
    @Query("""
        SELECT p FROM UserTopicProgress p
        WHERE p.userId = :userId
          AND p.status = 'done'
        ORDER BY p.lastReviewedAt ASC NULLS FIRST
        """)
    List<UserTopicProgress> findDoneOrderedByLastReviewed(@Param("userId") UUID userId);

    /**
     * Finds in_progress topics for the planner.
     */
    List<UserTopicProgress> findByUserIdAndStatus(UUID userId, String status);
}
