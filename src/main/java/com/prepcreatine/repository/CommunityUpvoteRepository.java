package com.prepcreatine.repository;

import com.prepcreatine.domain.CommunityUpvote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CommunityUpvoteRepository extends JpaRepository<CommunityUpvote, UUID> {

    Optional<CommunityUpvote> findByUserIdAndEntityIdAndEntityType(UUID userId, UUID entityId, String entityType);

    boolean existsByUserIdAndEntityIdAndEntityType(UUID userId, UUID entityId, String entityType);
}
