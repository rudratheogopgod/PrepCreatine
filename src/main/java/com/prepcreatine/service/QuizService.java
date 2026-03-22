package com.prepcreatine.service;

import com.prepcreatine.domain.*;
import com.prepcreatine.dto.request.SubmitTestRequest;
import com.prepcreatine.dto.response.*;
import com.prepcreatine.exception.*;
import com.prepcreatine.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.*;

/**
 * Quiz / test service per BSDD §13.
 *
 * Flow:
 * 1. POST /api/tests/start     → generates N AI questions, creates TestSession (STARTED)
 * 2. POST /api/tests/{id}/submit → scores answers, marks COMPLETED, triggers analysis
 * 3. GET /api/tests/{id}       → returns full result with correct answers exposed
 *
 * [SECURITY] correct_answer field is NEVER returned while test status = STARTED.
 */
@Service
@Transactional
public class QuizService {

    private static final Logger log = LoggerFactory.getLogger(QuizService.class);

    private final TestSessionRepository testSessionRepo;
    private final TestAnswerRepository  testAnswerRepo;
    private final QuestionRepository    questionRepo;
    private final UserTopicProgressRepository progressRepo;
    private final GeminiService         gemini;
    private final ObjectMapper          om;

    public QuizService(TestSessionRepository testSessionRepo,
                       TestAnswerRepository testAnswerRepo,
                       QuestionRepository questionRepo,
                       UserTopicProgressRepository progressRepo,
                       GeminiService gemini,
                       ObjectMapper om) {
        this.testSessionRepo = testSessionRepo;
        this.testAnswerRepo  = testAnswerRepo;
        this.questionRepo    = questionRepo;
        this.progressRepo    = progressRepo;
        this.gemini          = gemini;
        this.om              = om;
    }

    /**
     * Starts a new test session.
     * If sufficient questions exist in the question bank, samples them.
     * Otherwise, generates new AI questions and saves them.
     */
    public TestSessionResponse startTest(UUID userId, String examId,
                                         String subjectId, String topicId,
                                         int questionCount, int timeLimitMins) {
        List<Question> questions = questionRepo.findByExamIdAndSubjectIdAndTopicId(
            examId, subjectId, topicId, PageRequest.of(0, questionCount));

        if (questions.size() < questionCount) {
            questions = generateAndSaveQuestions(examId, subjectId, topicId, questionCount);
        }

        Collections.shuffle(questions);
        questions = questions.subList(0, Math.min(questionCount, questions.size()));

        TestSession session = new TestSession();
        session.setUserId(userId);
        session.setExamId(examId);
        session.setSubjectId(subjectId);
        session.setTopicId(topicId);
        session.setTotalQuestions(questions.size());
        session.setTimeLimitMins(timeLimitMins);
        session.setStatus("STARTED");
        testSessionRepo.save(session);

        // Store question IDs in session (for validation on submit)
        session.setQuestionIds(questions.stream().map(Question::getId).collect(java.util.stream.Collectors.toList()));
        testSessionRepo.save(session);

        return toDetailResponse(session, questions, false);
    }

    /**
     * Submits answers and scores the test.
     * Correct answers exposed ONLY after status = COMPLETED.
     */
    public TestSessionResponse submitTest(SubmitTestRequest req, UUID userId) {
        TestSession session = testSessionRepo.findById(req.testSessionId())
            .orElseThrow(() -> new ResourceNotFoundException("Test session not found."));

        if (!session.getUserId().equals(userId)) {
            throw new ForbiddenException("Not your test session.", userId);
        }
        if (!"STARTED".equals(session.getStatus())) {
            throw new ValidationException("This test has already been submitted.");
        }

        int correct = 0;
        List<TestAnswer> answers = new ArrayList<>();

        for (SubmitTestRequest.AnswerItem item : req.answers()) {
            Question q = questionRepo.findById(item.questionId())
                .orElseThrow(() -> new ResourceNotFoundException("Question not found: " + item.questionId()));

            boolean isCorrect = item.answer() != null && item.answer().equalsIgnoreCase(q.getCorrectAnswer());
            if (isCorrect) correct++;

            TestAnswer ta = new TestAnswer();
            ta.setTestSessionId(session.getId());
            ta.setQuestionId(q.getId());
            ta.setUserAnswer(item.answer());
            ta.setIsCorrect(isCorrect);
            ta.setTimeTakenSecs(item.timeTakenSecs());
            answers.add(ta);
        }
        testAnswerRepo.saveAll(answers);

        // Score
        BigDecimal score = session.getTotalQuestions() == 0 ? BigDecimal.ZERO
            : BigDecimal.valueOf(correct * 100.0 / session.getTotalQuestions())
                .setScale(2, RoundingMode.HALF_UP);

        session.setCorrectCount(correct);
        session.setAnsweredCount(req.answers().size());
        session.setScore(score);
        session.setStatus("COMPLETED");
        session.setSubmittedAt(OffsetDateTime.now());
        testSessionRepo.save(session);

        // Update topic progress async
        updateTopicProgressAsync(userId, session.getTopicId(), score.doubleValue());

        List<Question> questions = questionRepo.findAllById(
            session.getQuestionIds() != null ? session.getQuestionIds() : List.of());
        return toDetailResponse(session, questions, true);
    }

    @Transactional(readOnly = true)
    public List<TestSessionResponse> listForUser(UUID userId) {
        return testSessionRepo.findByUserIdOrderByCreatedAtDesc(userId)
            .stream()
            .map(s -> toSummaryResponse(s))
            .toList();
    }

    @Transactional(readOnly = true)
    public TestSessionResponse getById(UUID sessionId, UUID userId) {
        TestSession session = testSessionRepo.findByIdAndUserId(sessionId, userId)
            .orElseThrow(() -> new ResourceNotFoundException("Test session not found."));
        boolean completed = "COMPLETED".equals(session.getStatus());
        List<Question> questions = questionRepo.findAllById(
            session.getQuestionIds() != null ? session.getQuestionIds() : List.of());
        return toDetailResponse(session, questions, completed);
    }

    // ── Private helpers ───────────────────────────────────────────────────

    @Async
    protected void updateTopicProgressAsync(UUID userId, String topicId, double score) {
        if (topicId == null) return;
        try {
            progressRepo.findByUserIdAndTopicId(userId, topicId).ifPresentOrElse(p -> {
                p.setLastScore(BigDecimal.valueOf(score));
                p.setTestAttempts(p.getTestAttempts() + 1);
                p.setStatus(score >= 80 ? "mastered" : score >= 50 ? "in_progress" : "needs_review");
                progressRepo.save(p);
            }, () -> {
                UserTopicProgress p = new UserTopicProgress();
                p.setUserId(userId);
                p.setTopicId(topicId);
                p.setLastScore(BigDecimal.valueOf(score));
                p.setTestAttempts(1);
                p.setStatus(score >= 80 ? "mastered" : score >= 50 ? "in_progress" : "needs_review");
                progressRepo.save(p);
            });
        } catch (Exception e) {
            log.error("[Quiz] Topic progress update failed for userId={}: {}", userId, e.getMessage());
        }
    }

    private List<Question> generateAndSaveQuestions(String examId, String subjectId,
                                                     String topicId, int count) {
        String prompt = """
            Generate %d unique multiple-choice questions for:
            Exam: %s | Subject: %s | Topic: %s
            
            Return ONLY a JSON array with each object having:
            { "stem": "...", "options": ["A. ...", "B. ...", "C. ...", "D. ..."],
              "correctAnswer": "A", "explanation": "...", "level": 2 }
            """.formatted(count, examId, subjectId, topicId);

        String json = gemini.generateContent(
            "You are an expert question setter for " + examId + " examination. Return only valid JSON.", prompt);

        List<Question> generated = new ArrayList<>();
        try {
            com.fasterxml.jackson.databind.JsonNode arr = om.readTree(json);
            for (com.fasterxml.jackson.databind.JsonNode node : arr) {
                Question q = new Question();
                q.setExamId(examId);
                q.setSubjectId(subjectId);
                q.setTopicId(topicId);
                q.setType("MCQ");
                q.setQuestionText(node.path("stem").asText());
                q.setCorrectAnswer(node.path("correctAnswer").asText());
                q.setExplanation(node.path("explanation").asText());
                q.setLevel((short) node.path("level").asInt(2));
                // Store options as array string
                List<String> opts = new ArrayList<>();
                node.path("options").forEach(o -> opts.add(o.asText()));
                q.setOptionsArr(opts.toArray(new String[0]));
                generated.add(questionRepo.save(q));
            }
        } catch (Exception e) {
            log.error("[Quiz] Failed to parse AI questions: {}", e.getMessage());
            throw new ExternalServiceException("Gemini", "Failed to generate questions. Please try again.");
        }
        return generated;
    }

    private TestSessionResponse toDetailResponse(TestSession session, List<Question> questions, boolean showAnswers) {
        List<TestAnswerRepository.AnswerProjection> answersMap = showAnswers
            ? testAnswerRepo.findByTestSessionId(session.getId())
            : List.of();

        Map<UUID, TestAnswerRepository.AnswerProjection> answerByQ = new HashMap<>();
        answersMap.forEach(a -> answerByQ.put(a.getQuestionId(), a));

        List<QuestionResponse> qrs = questions.stream().map(q -> {
            var ans = answerByQ.get(q.getId());
            return new QuestionResponse(
                q.getId(), q.getTopicId(), q.getSubjectId(), q.getExamId(),
                q.getType(), q.getLevel(), q.getQuestionText(),
                q.getOptionsArr() != null ? java.util.Arrays.asList(q.getOptionsArr()) : java.util.List.of(),
                showAnswers ? q.getCorrectAnswer() : null,
                showAnswers ? q.getExplanation() : null,
                ans != null ? ans.getUserAnswer() : null,
                showAnswers && ans != null ? ans.getIsCorrect() : null,
                ans != null ? ans.getTimeTakenSecs() : 0
            );
        }).toList();

        return new TestSessionResponse(
            session.getId(), session.getExamId(), session.getSubjectId(),
            session.getTopicId(), session.getStatus(),
            session.getTotalQuestions(), session.getAnsweredCount(),
            session.getCorrectCount(), session.getScore(),
            session.getTimeLimitMins(),
            session.getCreatedAt(), session.getSubmittedAt(), qrs);
    }

    private TestSessionResponse toSummaryResponse(TestSession session) {
        return new TestSessionResponse(
            session.getId(), session.getExamId(), session.getSubjectId(),
            session.getTopicId(), session.getStatus(),
            session.getTotalQuestions(), session.getAnsweredCount(),
            session.getCorrectCount(), session.getScore(),
            session.getTimeLimitMins(),
            session.getCreatedAt(), session.getSubmittedAt(), null);
    }
}
