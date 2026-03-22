package com.prepcreatine.repository;

import com.prepcreatine.domain.UserTopicProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface UserTopicProgressRepository extends JpaRepository<UserTopicProgress, UUID> {
    
    List<UserTopicProgress> findByUserId(UUID userId);

    java.util.Optional<UserTopicProgress> findByUserIdAndTopicId(UUID userId, String topicId);
    
    long countByUserId(UUID userId);
    
    long countByUserIdAndStatus(UUID userId, String status);
}
