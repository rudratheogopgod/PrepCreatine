package com.prepcreatine.service;

import com.prepcreatine.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;

/**
 * Analytics service per BSDD §12.
 * Provides dashboard analytics: activity heatmap, streak info,
 * topic breakdown, test performance over time, readiness score.
 */
@Service
@Transactional(readOnly = true)
public class AnalyticsService {

    private final SessionRepository          sessionRepo;
    private final UserTopicProgressRepository progressRepo;
    private final TestSessionRepository      testRepo;
    private final UserRepository             userRepo;

    public AnalyticsService(SessionRepository sessionRepo,
                            UserTopicProgressRepository progressRepo,
                            TestSessionRepository testRepo,
                            UserRepository userRepo) {
        this.sessionRepo  = sessionRepo;
        this.progressRepo = progressRepo;
        this.testRepo     = testRepo;
        this.userRepo     = userRepo;
    }

    /**
     * 365-day activity heatmap — used in GitHub-style calendar widget.
     * Returns Map<date, studyMins>.
     */
    public Map<LocalDate, Integer> getHeatmap(UUID userId) {
        LocalDate from = LocalDate.now().minusDays(364);
        List<SessionRepository.DailyStudy> rows = sessionRepo.findDailyStudyMins(userId, from);
        Map<LocalDate, Integer> heatmap = new LinkedHashMap<>();
        rows.forEach(r -> heatmap.put(r.getDate(), r.getDurationMins()));
        return heatmap;
    }

    /**
     * Topic progress breakdown — used in subject breakdown chart.
     * Returns Map<topicId, status>.
     */
    public Map<String, String> getTopicProgress(UUID userId) {
        Map<String, String> result = new LinkedHashMap<>();
        progressRepo.findByUserId(userId)
            .forEach(p -> result.put(p.getTopicId(), p.getStatus()));
        return result;
    }

    /**
     * Test performance over time — used in score trend line chart.
     * Returns list of {date, score} for last 30 completed tests.
     */
    public List<Map<String, Object>> getTestPerformance(UUID userId) {
        return testRepo.findCompletedByUserIdOrderByDate(userId, 30)
            .stream()
            .map(s -> Map.<String, Object>of(
                "date",  s.getSubmittedAt().toLocalDate().toString(),
                "score", s.getScore()
            ))
            .toList();
    }

    /**
     * Summary stats for dashboard cards.
     */
    public Map<String, Object> getSummary(UUID userId) {
        var user = userRepo.findByIdAndIsActiveTrue(userId)
            .orElseThrow(() -> new com.prepcreatine.exception.ResourceNotFoundException("User not found."));

        long totalTopics   = progressRepo.countByUserId(userId);
        long mastered      = progressRepo.countByUserIdAndStatus(userId, "mastered");
        long testsCompleted = testRepo.countByUserIdAndStatus(userId, "COMPLETED");
        Double avgScore    = testRepo.findAvgScoreForUser(userId);

        return Map.of(
            "currentStreak",   user.getCurrentStreak(),
            "longestStreak",   user.getLongestStreak(),
            "totalDays",       user.getTotalDays(),
            "readinessScore",  user.getReadinessScore(),
            "topicsTotal",     totalTopics,
            "topicsMastered",  mastered,
            "testsCompleted",  testsCompleted,
            "avgTestScore",    avgScore != null ? avgScore : 0.0
        );
    }
}
