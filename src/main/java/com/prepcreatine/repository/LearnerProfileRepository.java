package com.prepcreatine.repository;

import com.prepcreatine.domain.LearnerProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for the learner_profiles table.
 * Key query: findUsersNeedingReanalysis — drives the LearnerAnalysisAgent schedule.
 */
@Repository
public interface LearnerProfileRepository extends JpaRepository<LearnerProfile, UUID> {

    Optional<LearnerProfile> findByUserId(UUID userId);

    /**
     * Returns all user IDs that either have never been analyzed OR
     * were last analyzed before the given cutoff instant.
     * Used by LearnerAnalysisAgent to find users needing a fresh analysis run.
     */
    @Query("""
        SELECT p.userId FROM LearnerProfile p
        WHERE p.lastAnalyzedAt IS NULL
           OR p.lastAnalyzedAt < :cutoff
        """)
    List<UUID> findUsersNeedingReanalysis(@Param("cutoff") Instant cutoff);
}
