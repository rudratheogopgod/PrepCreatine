package com.prepcreatine.service;

import com.prepcreatine.domain.*;
import com.prepcreatine.repository.*;
import com.prepcreatine.util.EvalLogger;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * LearnerAnalysisAgent — Component B: The Reasoning Engine
 *
 * Runs every 2 hours as an autonomous scheduled agent.
 * Reads the LearnerProfile (memory), calls Gemini to reason about patterns,
 * then writes back actionable insights (weakness_pattern, recommended_mode).
 * These insights then flow into every AI call automatically — closing the
 * Observe → Remember → REASON → Act agent loop.
 */
@Component
public class LearnerAnalysisAgent {

    private static final Logger log = LoggerFactory.getLogger(LearnerAnalysisAgent.class);

    private final LearnerProfileRepository  profileRepo;
    private final ConceptStruggleRepository conceptStruggleRepo;
    private final UserContextRepository     userContextRepo;
    private final NotificationService       notificationService;
    private final GeminiService             geminiService;
    private final ObjectMapper              objectMapper;

    public LearnerAnalysisAgent(LearnerProfileRepository profileRepo,
                                ConceptStruggleRepository conceptStruggleRepo,
                                UserContextRepository userContextRepo,
                                NotificationService notificationService,
                                GeminiService geminiService,
                                ObjectMapper objectMapper) {
        this.profileRepo         = profileRepo;
        this.conceptStruggleRepo = conceptStruggleRepo;
        this.userContextRepo     = userContextRepo;
        this.notificationService = notificationService;
        this.geminiService       = geminiService;
        this.objectMapper        = objectMapper;
    }

    /**
     * Main agent loop: runs every 2 hours automatically.
     * Finds users needing re-analysis and calls analyzeUser() for each.
     */
    @Scheduled(fixedDelay = 2 * 60 * 60 * 1000)  // every 2 hours
    @Async
    public void runAnalysisCycle() {
        log.info("[LearnerAgent] Scheduled analysis cycle starting...");
        try {
            Instant cutoff = Instant.now().minus(2, ChronoUnit.HOURS);
            List<UUID> userIds = profileRepo.findUsersNeedingReanalysis(cutoff);
            log.info("[LearnerAgent] Found {} users needing analysis", userIds.size());

            for (UUID userId : userIds) {
                try {
                    analyzeUser(userId);
                    Thread.sleep(200); // rate limit — 5 per second max
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    EvalLogger.failure(log, "LEARNER-AGENT",
                        "Analysis failed userId=" + userId + ": " + e.getMessage());
                }
            }
            log.info("[LearnerAgent] Cycle complete: {} users analyzed", userIds.size());
        } catch (Exception e) {
            log.error("[LearnerAgent] Cycle error: {}", e.getMessage());
        }
    }

    /**
     * Analyzes a single user: reads behavioral profile + struggles,
     * calls Gemini for reasoning, writes back insights.
     * Can also be called on-demand via {@code /api/agent/analyze}.
     */
    @Async
    @Transactional
    public void analyzeUser(UUID userId) {
        LearnerProfile profile = profileRepo.findByUserId(userId).orElse(null);
        if (profile == null) return;

        List<ConceptStruggle> struggles = conceptStruggleRepo
            .findTopByUserIdOrderByStruggleCountDesc(userId, 10);
        UserContext ctx = userContextRepo.findByUserId(userId).orElse(null);
        if (ctx == null) return;

        EvalLogger.separator(log);
        EvalLogger.agentAction(log, "LEARNER-AGENT",
            "Analyzing learner profile for userId=" + userId);
        EvalLogger.result(log, "LEARNER-AGENT", "Struggle indicator",
            String.format("%.0f%%",
                profile.getStruggleIndicator() != null
                    ? profile.getStruggleIndicator().doubleValue() * 100 : 50));
        EvalLogger.result(log, "LEARNER-AGENT", "Consistency score",
            String.format("%.0f%%",
                profile.getConsistencyScore() != null
                    ? profile.getConsistencyScore().doubleValue() * 100 : 0));
        if (!struggles.isEmpty()) {
            String topStruggles = struggles.stream()
                .map(s -> s.getConceptTag() + "(" + s.getStruggleCount() + "x)")
                .collect(Collectors.joining(", "));
            EvalLogger.result(log, "LEARNER-AGENT", "Top concept struggles", topStruggles);
        }

        String prompt   = buildAnalysisPrompt(profile, struggles, ctx);
        String response = geminiService.generateContent("", prompt);

        try {
            String json = extractJson(response);
            LearnerAnalysisResult result = objectMapper.readValue(json,
                LearnerAnalysisResult.class);

            String currentMode = ctx.getStudyMode() != null ? ctx.getStudyMode() : "in_depth";

            profile.setWeaknessPattern(result.weaknessPattern());
            profile.setStrengthPattern(result.strengthPattern());
            profile.setRecommendedMode(result.recommendedMode());
            profile.setLearningVelocity(BigDecimal.valueOf(result.learningVelocity()));
            profile.setLastAnalyzedAt(Instant.now());
            profileRepo.save(profile);

            EvalLogger.success(log, "LEARNER-AGENT",
                "Analysis complete: weakness='" + result.weaknessPattern() + "'");
            EvalLogger.result(log, "LEARNER-AGENT", "Recommended mode", result.recommendedMode());
            EvalLogger.result(log, "LEARNER-AGENT", "Learning velocity",
                String.format("%.0f%%", result.learningVelocity() * 100));

            // Suggest mode change via notification if the mode differs
            if (result.recommendedMode() != null &&
                !result.recommendedMode().equals(currentMode)) {
                EvalLogger.agentAction(log, "LEARNER-AGENT",
                    "Mode change recommended: " + currentMode + " \u2192 " + result.recommendedMode());
                EvalLogger.agentAction(log, "LEARNER-AGENT",
                    "Notification queued for student about mode change");
                notificationService.createNotification(userId, "ai_insight",
                    "Study mode recommendation",
                    "Based on your recent performance, switching to '" +
                    result.recommendedMode() + "' mode might help you improve faster.",
                    "/settings");
            }
            EvalLogger.separator(log);

        } catch (Exception e) {
            EvalLogger.failure(log, "LEARNER-AGENT",
                "Parse failed userId=" + userId + ": " + e.getMessage());
        }
    }

    // ── Private helpers ────────────────────────────────────────────────────

    private String buildAnalysisPrompt(LearnerProfile profile,
                                        List<ConceptStruggle> struggles,
                                        UserContext ctx) {
        String struggleList = struggles.stream()
            .map(s -> s.getConceptTag() + " (seen " + s.getStruggleCount() + " times)")
            .collect(Collectors.joining(", "));

        long daysToExam = ctx.getExamDate() != null
            ? ChronoUnit.DAYS.between(LocalDate.now(), ctx.getExamDate()) : -1;

        return String.format("""
            Analyze a student's learning patterns to identify specific weaknesses
            and provide coaching recommendations.

            STUDENT DATA:
            - Exam: %s, Days to exam: %s
            - Struggle indicator: %.2f (0=easy, 1=maximum difficulty)
            - Avg time on wrong answers: %.0f seconds
            - Avg time on correct answers: %.0f seconds
            - Consistency (days active/14): %.0f%%
            - Total questions attempted: %d
            - Concept areas with repeated mistakes: %s
            - Current study mode: %s

            Return ONLY valid JSON, no markdown, no preamble:
            {
              "weaknessPattern": "One specific sentence about main weakness (mention actual concepts)",
              "strengthPattern": "One specific sentence about what they do well",
              "recommendedMode": "in_depth OR revision OR speed_run OR overview",
              "learningVelocity": 0.6,
              "coachingInsight": "Two-sentence specific coaching advice",
              "priorityConceptAreas": ["concept1", "concept2", "concept3"]
            }

            Rules for recommendedMode:
            - If struggle > 0.5 AND daysToExam < 30: "speed_run"
            - If struggle > 0.5 AND daysToExam > 30: "in_depth"
            - If struggle < 0.3: "revision"
            - Otherwise: "in_depth"
            learningVelocity: 0.0 (very slow) to 1.0 (very fast)
            """,
            ctx.getExamType() != null ? ctx.getExamType().toUpperCase() : "JEE",
            daysToExam > 0 ? daysToExam + " days" : "not set",
            profile.getStruggleIndicator()  != null ? profile.getStruggleIndicator().doubleValue() : 0.5,
            profile.getAvgTimePerWrong()    != null ? profile.getAvgTimePerWrong().doubleValue() : 60,
            profile.getAvgTimePerCorrect()  != null ? profile.getAvgTimePerCorrect().doubleValue() : 40,
            profile.getConsistencyScore()   != null ? profile.getConsistencyScore().doubleValue() * 100 : 50,
            profile.getTotalQuestionsSeen() != null ? profile.getTotalQuestionsSeen() : 0,
            struggleList.isEmpty() ? "none yet" : struggleList,
            ctx.getStudyMode() != null ? ctx.getStudyMode() : "in_depth"
        );
    }

    /** Extract the first {...} JSON block from Gemini's response. */
    private String extractJson(String response) {
        if (response == null) throw new IllegalArgumentException("null response");
        int start = response.indexOf('{');
        int end   = response.lastIndexOf('}');
        if (start == -1 || end == -1 || end < start)
            throw new IllegalArgumentException("No JSON found in response");
        return response.substring(start, end + 1);
    }

    /** Jackson-mapped DTO for Gemini's analysis response. */
    public record LearnerAnalysisResult(
        String weaknessPattern,
        String strengthPattern,
        String recommendedMode,
        double learningVelocity,
        String coachingInsight,
        List<String> priorityConceptAreas
    ) {}
}
