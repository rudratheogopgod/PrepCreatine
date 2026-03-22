package com.prepcreatine.service;

import com.prepcreatine.domain.*;
import com.prepcreatine.repository.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * AdaptiveQuizService — generates targeted-practice drill sessions
 * for topics where the student scored poorly (<50%).
 *
 * Triggered asynchronously from QuizService.submitTest() after grading.
 * Creates 3 AI-generated questions targeting the same underlying concepts
 * that the student got wrong, then creates a targeted_practice TestSession.
 */
@Service
public class AdaptiveQuizService {

    private static final Logger log = LoggerFactory.getLogger(AdaptiveQuizService.class);

    private final GeminiService             gemini;
    private final QuestionRepository        questionRepo;
    private final TestSessionRepository     testSessionRepo;
    private final TestAnswerRepository      testAnswerRepo;
    private final NotificationService       notificationService;
    private final ObjectMapper              om;

    public AdaptiveQuizService(GeminiService gemini,
                               QuestionRepository questionRepo,
                               TestSessionRepository testSessionRepo,
                               TestAnswerRepository testAnswerRepo,
                               NotificationService notificationService,
                               ObjectMapper om) {
        this.gemini              = gemini;
        this.questionRepo        = questionRepo;
        this.testSessionRepo     = testSessionRepo;
        this.testAnswerRepo      = testAnswerRepo;
        this.notificationService = notificationService;
        this.om                  = om;
    }

    /**
     * Generates a 3-question targeted drill for a topic the student failed.
     * Runs @Async — does not block the test submission response.
     *
     * @param userId       the student
     * @param topicId      which topic to drill
     * @param examId       the exam type (jee / neet)
     * @param wrongAnswers the answers the student got wrong in the failed session
     */
    @Async
    @Transactional
    public void generateTargetedDrill(UUID userId, String topicId, String examId,
                                      List<TestAnswer> wrongAnswers) {
        try {
            log.info("[AdaptiveDrill] Generating drill: userId={}, topic={}", userId, topicId);

            String topicName  = getTopicName(topicId);
            String subjectId  = getSubjectId(topicId);

            // 1. Build context from wrong answers
            String wrongCtx = wrongAnswers.stream()
                .limit(3)
                .map(a -> {
                    Question q = questionRepo.findById(a.getQuestionId()).orElse(null);
                    if (q == null) return "- unknown question";
                    return String.format("- Q: %s | Student answered: %s | Correct: %s",
                        q.getQuestionText() != null ? q.getQuestionText().substring(0, Math.min(100, q.getQuestionText().length())) : "?",
                        a.getUserAnswer() != null ? a.getUserAnswer() : "blank",
                        q.getCorrectAnswer());
                })
                .collect(Collectors.joining("\n"));

            // 2. Build Gemini prompt
            String prompt = """
                A student preparing for %s got these questions wrong on '%s':
                %s

                Generate exactly 3 new MCQ questions that test the SAME underlying concepts
                from a completely different angle. Use JEE/NEET PYQ-style.
                Each must have exactly 4 options (A/B/C/D) and one correct answer.

                Return ONLY valid JSON array. No markdown. No preamble. Start with [
                [
                  {
                    "questionText": "Full question text",
                    "optionA": "First option text",
                    "optionB": "Second option text",
                    "optionC": "Third option text",
                    "optionD": "Fourth option text",
                    "correctAnswer": "A",
                    "explanation": "Brief explanation of the correct answer"
                  }
                ]
                """.formatted(examId.toUpperCase(), topicName, wrongCtx);

            // 3. Call Gemini
            String raw = gemini.generateContent(
                "You are an expert question setter. Return only valid JSON.", prompt);
            raw = raw.strip();
            if (raw.startsWith("```")) raw = raw.replaceAll("```json|```", "").strip();

            List<Map<String, String>> dtos = om.readValue(raw,
                new TypeReference<List<Map<String, String>>>() {});

            // 4. Persist questions
            List<UUID> questionIds = new ArrayList<>();
            for (Map<String, String> dto : dtos) {
                Question q = new Question();
                q.setExamId(examId);
                q.setTopicId(topicId);
                q.setSubjectId(subjectId);
                q.setLevel((short) 2);
                q.setType("mcq");
                q.setQuestionText(dto.getOrDefault("questionText", "Question text missing"));
                q.setCorrectAnswer(dto.getOrDefault("correctAnswer", "A"));
                q.setExplanation(dto.getOrDefault("explanation", ""));
                q.setAiGenerated(true);
                q.setSourceRef("AI-generated targeted drill");
                // Store options
                String[] opts = {
                    dto.getOrDefault("optionA", "A"),
                    dto.getOrDefault("optionB", "B"),
                    dto.getOrDefault("optionC", "C"),
                    dto.getOrDefault("optionD", "D")
                };
                q.setOptionsArr(opts);
                questionIds.add(questionRepo.save(q).getId());
            }

            // 5. Create targeted_practice session
            TestSession drill = new TestSession();
            drill.setUserId(userId);
            drill.setExamId(examId);
            drill.setTopicId(topicId);
            drill.setSubjectId(subjectId);
            drill.setTestType("targeted_practice");
            drill.setLevel((short) 2);
            drill.setStatus("in_progress");
            drill.setTotalQuestions(questionIds.size());
            drill.setTimeLimitMins(10);
            drill.setQuestionIds(questionIds);
            TestSession saved = testSessionRepo.save(drill);

            // 6. Notify student
            notificationService.createNotification(
                userId,
                "test_result",
                "Targeted drill ready: " + topicName,
                "3 new questions to close your concept gap. Practice now!",
                "/test/" + saved.getId()
            );

            log.info("[AdaptiveDrill] Created: sessionId={}, topic={}", saved.getId(), topicId);

        } catch (Exception e) {
            log.warn("[AdaptiveDrill] Failed: userId={}, topic={}, error={}",
                userId, topicId, e.getMessage(), e);
        }
    }

    /**
     * Human-readable topic name lookup.
     */
    private String getTopicName(String topicId) {
        Map<String, String> names = new HashMap<>(Map.of(
            "jee-chemistry-organic-goc",    "General Organic Chemistry",
            "jee-math-integration",          "Integration",
            "jee-physics-thermodynamics",    "Thermodynamics",
            "jee-physics-optics",            "Optics",
            "jee-physics-kinematics",        "Kinematics",
            "jee-chemistry-electrochemistry","Electrochemistry",
            "jee-math-probability",          "Probability",
            "jee-math-coordinates",          "Coordinate Geometry",
            "neet-biology-genetics",         "Genetics",
            "neet-biology-cell",             "Cell Structure"
        ));
        if (names.containsKey(topicId)) return names.get(topicId);
        // Fallback: convert "jee-physics-some-topic" → "Some Topic"
        String[] parts = topicId.split("-");
        return Arrays.stream(parts)
            .skip(2)
            .map(p -> Character.toUpperCase(p.charAt(0)) + p.substring(1))
            .collect(Collectors.joining(" "));
    }

    private String getSubjectId(String topicId) {
        if (topicId == null) return "general";
        if (topicId.contains("-physics-"))   return "physics";
        if (topicId.contains("-chemistry-")) return "chemistry";
        if (topicId.contains("-math-"))      return "mathematics";
        if (topicId.contains("-biology-"))   return "biology";
        return "general";
    }
}
