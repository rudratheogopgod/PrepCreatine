package com.prepcreatine.service;

import com.prepcreatine.domain.*;
import com.prepcreatine.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * LearnerProfileService — Component A: The Persistent Memory
 *
 * Maintains a continuously updated behavioral model of the student.
 * Called after EVERY test submission and study session.
 * Output (buildProfileSummary) is injected into EVERY AI call automatically.
 *
 * Observe → Remember (this class) → Reason (LearnerAnalysisAgent) → Act (planner/chat)
 */
@Service
@Transactional
public class LearnerProfileService {

    private static final Logger log = LoggerFactory.getLogger(LearnerProfileService.class);

    private final LearnerProfileRepository     profileRepo;
    private final ConceptStruggleRepository    conceptStruggleRepo;
    private final TestSessionRepository        sessionRepo;

    public LearnerProfileService(LearnerProfileRepository profileRepo,
                                 ConceptStruggleRepository conceptStruggleRepo,
                                 TestSessionRepository sessionRepo) {
        this.profileRepo         = profileRepo;
        this.conceptStruggleRepo = conceptStruggleRepo;
        this.sessionRepo         = sessionRepo;
    }

    /**
     * Called after EVERY test submission — updates behavioral stats.
     * Rolling averages with weight 0.3 new / 0.7 old for stability.
     */
    @Async
    public void updateFromTestResult(UUID userId,
                                     List<TestAnswer> answers,
                                     List<Question> questions) {
        try {
            LearnerProfile profile = profileRepo.findByUserId(userId)
                .orElse(new LearnerProfile(userId));

            // 1. Avg time for correct vs wrong answers
            double avgCorrectTime = answers.stream()
                .filter(a -> Boolean.TRUE.equals(a.getIsCorrect()))
                .mapToInt(TestAnswer::getTimeTakenSecs)
                .average().orElse(0);
            double avgWrongTime = answers.stream()
                .filter(a -> Boolean.FALSE.equals(a.getIsCorrect()))
                .mapToInt(TestAnswer::getTimeTakenSecs)
                .average().orElse(0);

            // Rolling average
            if (profile.getAvgTimePerCorrect().doubleValue() == 0) {
                profile.setAvgTimePerCorrect(BigDecimal.valueOf(avgCorrectTime));
                profile.setAvgTimePerWrong(BigDecimal.valueOf(avgWrongTime));
            } else {
                profile.setAvgTimePerCorrect(BigDecimal.valueOf(
                    0.3 * avgCorrectTime + 0.7 * profile.getAvgTimePerCorrect().doubleValue()));
                profile.setAvgTimePerWrong(BigDecimal.valueOf(
                    0.3 * avgWrongTime + 0.7 * profile.getAvgTimePerWrong().doubleValue()));
            }

            // 2. Struggle indicator (rolling, 0.3 new / 0.7 old)
            long totalAnswers = answers.size();
            long wrongAnswers = answers.stream()
                .filter(a -> Boolean.FALSE.equals(a.getIsCorrect())).count();
            double newStruggle = totalAnswers > 0 ? (double) wrongAnswers / totalAnswers : 0;
            double currentStruggle = profile.getStruggleIndicator() != null
                ? profile.getStruggleIndicator().doubleValue() : 0;
            profile.setStruggleIndicator(BigDecimal.valueOf(
                0.3 * newStruggle + 0.7 * currentStruggle));

            // 3. Track concept-level struggles from wrong answers
            answers.stream()
                .filter(a -> Boolean.FALSE.equals(a.getIsCorrect()))
                .forEach(answer -> questions.stream()
                    .filter(q -> q.getId().equals(answer.getQuestionId()))
                    .findFirst()
                    .ifPresent(q -> {
                        List<String> tags = extractConceptTags(
                            q.getQuestionText(), q.getTopicId());
                        tags.forEach(tag ->
                            conceptStruggleRepo.upsertStruggle(
                                userId, q.getTopicId(), tag));
                    }));

            // 4. Update total counts
            profile.setTotalQuestionsSeenOrDefault(
                profile.getTotalQuestionsSeen() + (int) totalAnswers);

            profileRepo.save(profile);
            log.info("[LearnerProfile] Updated: userId={}, struggle={:.3f}, avgWrongTime={:.0f}s",
                userId, profile.getStruggleIndicator(), avgWrongTime);

        } catch (Exception e) {
            log.warn("[LearnerProfile] Update failed: userId={}, error={}",
                userId, e.getMessage());
        }
    }

    /**
     * Called after EVERY chat interaction / study session.
     * Updates consistency score based on days active in last 14.
     */
    @Async
    public void updateFromStudySession(UUID userId, int durationMins,
                                       List<String> topicsTouched) {
        try {
            LearnerProfile profile = profileRepo.findByUserId(userId)
                .orElse(new LearnerProfile(userId));

            profile.setTotalStudySessions(profile.getTotalStudySessions() + 1);

            // Consistency score: distinct study days in last 14 / 14
            long distinctDays = sessionRepo
                .countDistinctStudyDatesByUserIdInLast14Days(userId);
            profile.setConsistencyScore(
                BigDecimal.valueOf(Math.min(1.0, distinctDays / 14.0)));

            profileRepo.save(profile);
        } catch (Exception e) {
            log.warn("[LearnerProfile] Session update failed: {}", e.getMessage());
        }
    }

    /**
     * Returns the current learner profile as a compact summary string for AI prompts.
     * This string flows into EVERY AI call via PromptBuilderService.
     */
    @Transactional(readOnly = true)
    public String buildProfileSummary(UUID userId) {
        return profileRepo.findByUserId(userId).map(p -> String.format(
            "LEARNER PROFILE: struggle_indicator=%.2f (0=easy,1=hard), " +
            "consistency=%.0f%% days active last 2 weeks, " +
            "avg_time_wrong=%.0fs (normal=60s), " +
            "weakness_pattern='%s', strength_pattern='%s', " +
            "recommended_mode=%s. " +
            "Top concept struggles: %s",
            p.getStruggleIndicator() != null ? p.getStruggleIndicator().doubleValue() : 0.5,
            p.getConsistencyScore()  != null ? p.getConsistencyScore().doubleValue() * 100 : 50,
            p.getAvgTimePerWrong()   != null ? p.getAvgTimePerWrong().doubleValue() : 60,
            p.getWeaknessPattern()   != null ? p.getWeaknessPattern() : "not yet analyzed",
            p.getStrengthPattern()   != null ? p.getStrengthPattern() : "not yet analyzed",
            p.getRecommendedMode()   != null ? p.getRecommendedMode() : "in_depth",
            getTopStrugglesString(userId)
        )).orElse("LEARNER PROFILE: new student, no data yet");
    }

    @Transactional(readOnly = true)
    public Optional<LearnerProfile> getProfile(UUID userId) {
        return profileRepo.findByUserId(userId);
    }

    // ── Private helpers ────────────────────────────────────────────────────

    private String getTopStrugglesString(UUID userId) {
        return conceptStruggleRepo
            .findTopByUserIdOrderByStruggleCountDesc(userId, 3)
            .stream()
            .map(s -> s.getConceptTag() + "(" + s.getStruggleCount() + "x)")
            .collect(Collectors.joining(", "));
    }

    /**
     * Heuristic concept tag extraction from question text.
     * These tags feed concept_struggles and drive agentic RAG query enrichment.
     */
    public List<String> extractConceptTags(String questionText, String topicId) {
        List<String> tags = new ArrayList<>();
        String lower = questionText != null ? questionText.toLowerCase() : "";

        if (lower.contains("rate") || lower.contains("derivative") ||
            lower.contains("differentiat") || lower.contains("d/dt") ||
            lower.contains("velocity") || lower.contains("acceleration"))
            tags.add("rate_of_change");

        if (lower.contains("3d") || lower.contains("three dimensional") ||
            lower.contains("vector") || lower.contains("cross product") ||
            lower.contains("dot product"))
            tags.add("3d_spatial");

        if (lower.contains("equilibrium") || lower.contains("balance") ||
            lower.contains("steady state") || lower.contains("constant"))
            tags.add("equilibrium_reasoning");

        if (lower.contains("electron") || lower.contains("charge") ||
            lower.contains("inductive") || lower.contains("mesomeric") ||
            lower.contains("resonance"))
            tags.add("electron_movement");

        if (lower.contains("integral") || lower.contains("area under") ||
            lower.contains("antiderivative"))
            tags.add("integration");

        if (lower.contains("energy") || lower.contains("conservation") ||
            lower.contains("work done"))
            tags.add("energy_conservation");

        // Always add topicId itself
        if (topicId != null) tags.add(topicId);

        return tags.isEmpty() && topicId != null ? List.of(topicId) : tags;
    }
}
