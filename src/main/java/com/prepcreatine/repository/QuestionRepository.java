package com.prepcreatine.repository;

import com.prepcreatine.domain.Question;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface QuestionRepository extends JpaRepository<Question, UUID> {

    List<Question> findByExamIdAndSubjectIdAndTopicId(
        String examId, String subjectId, String topicId, Pageable pageable);

    List<Question> findByExamIdAndSubjectIdAndTopicIdAndLevel(
        String examId, String subjectId, String topicId, Short level);

    List<Question> findByExamIdAndSubjectId(
        String examId, String subjectId, Pageable pageable);

    long countByExamIdAndSubjectIdAndTopicId(
        String examId, String subjectId, String topicId);
}
