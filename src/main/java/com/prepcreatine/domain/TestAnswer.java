package com.prepcreatine.domain;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * TestAnswer entity — maps to `test_answers` table.
 * One row per question answered in a test session.
 * is_correct is null if the question was unattempted (user_answer is also null).
 */
@Entity
@Table(name = "test_answers")
public class TestAnswer {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "test_session_id", nullable = false)
    private UUID testSessionId;

    @Column(name = "question_id", nullable = false)
    private UUID questionId;

    @Column(name = "user_answer", length = 500)
    private String userAnswer; // null = unattempted

    @Column(name = "is_correct")
    private Boolean isCorrect; // null = unattempted

    @Column(name = "time_taken_secs", nullable = false)
    private int timeTakenSecs = 0;

    @Column(name = "marked_review", nullable = false)
    private boolean markedReview = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    public TestAnswer() {}

    public UUID getId() { return id; }
    public UUID getTestSessionId() { return testSessionId; }
    public void setTestSessionId(UUID testSessionId) { this.testSessionId = testSessionId; }
    public UUID getQuestionId() { return questionId; }
    public void setQuestionId(UUID questionId) { this.questionId = questionId; }
    public String getUserAnswer() { return userAnswer; }
    public void setUserAnswer(String userAnswer) { this.userAnswer = userAnswer; }
    public Boolean getIsCorrect() { return isCorrect; }
    public void setIsCorrect(Boolean isCorrect) { this.isCorrect = isCorrect; }
    public int getTimeTakenSecs() { return timeTakenSecs; }
    public void setTimeTakenSecs(int timeTakenSecs) { this.timeTakenSecs = timeTakenSecs; }
    public boolean isMarkedReview() { return markedReview; }
    public void setMarkedReview(boolean markedReview) { this.markedReview = markedReview; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
}
