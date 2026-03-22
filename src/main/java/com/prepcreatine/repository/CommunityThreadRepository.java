package com.prepcreatine.repository;

import com.prepcreatine.domain.CommunityThread;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface CommunityThreadRepository extends JpaRepository<CommunityThread, UUID> {
    
    @Query("SELECT t FROM CommunityThread t WHERE " +
           "(:examId IS NULL OR t.examId = :examId) AND " +
           "(:subjectId IS NULL OR t.subjectId = :subjectId) AND " +
           "(:topicId IS NULL OR t.topicId = :topicId)")
    Page<CommunityThread> findByFilters(String examId, String subjectId, String topicId, Pageable pageable);
}
