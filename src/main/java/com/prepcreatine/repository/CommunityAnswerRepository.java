package com.prepcreatine.repository;

import com.prepcreatine.domain.CommunityAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CommunityAnswerRepository extends JpaRepository<CommunityAnswer, UUID> {
    
    List<CommunityAnswer> findByThreadId(UUID threadId);
    
    // Optionally we might need ordering:
    // List<CommunityAnswer> findByThreadIdOrderByUpvoteCountDesc(UUID threadId);
}
