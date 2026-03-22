package com.prepcreatine.repository;

import com.prepcreatine.domain.SystemSource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SystemSourceRepository extends JpaRepository<SystemSource, UUID> {

    List<SystemSource> findByExamId(String examId);

    List<SystemSource> findByExamIdAndSubjectId(String examId, String subjectId);

    long countByExamId(String examId);
}
