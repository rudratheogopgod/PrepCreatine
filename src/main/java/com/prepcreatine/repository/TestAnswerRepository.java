package com.prepcreatine.repository;

import com.prepcreatine.domain.TestAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TestAnswerRepository extends JpaRepository<TestAnswer, UUID> {
    
    interface AnswerProjection {
        UUID getQuestionId();
        String getUserAnswer();
        Boolean getIsCorrect();
        Integer getTimeTakenSecs();
    }
    
    List<AnswerProjection> findByTestSessionId(UUID testSessionId);
}
