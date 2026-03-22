package com.prepcreatine.controller;

import com.prepcreatine.service.AnalyticsService;
import com.prepcreatine.service.UserService;
import com.prepcreatine.util.SecurityUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Analytics endpoints per BSDD §12.
 *
 * GET /api/analytics/summary
 * GET /api/analytics/heatmap
 * GET /api/analytics/topics
 * GET /api/analytics/test-performance
 */
@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

    private final AnalyticsService analyticsService;
    private final UserService      userService;

    public AnalyticsController(AnalyticsService analyticsService, UserService userService) {
        this.analyticsService = analyticsService;
        this.userService      = userService;
    }

    /**
     * GET /api/analytics — composite endpoint for the Analytics dashboard page.
     * Combines summary stats, heatmap, topic progress, and test performance
     * into one response so the frontend only needs a single API call.
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getAll() {
        UUID userId = SecurityUtil.getCurrentUserId();
        Map<String, Object> summary     = analyticsService.getSummary(userId);
        Map<LocalDate, Integer> rawHeatmap = analyticsService.getHeatmap(userId);
        Map<String, String>  topics     = analyticsService.getTopicProgress(userId);
        List<Map<String, Object>> tests = analyticsService.getTestPerformance(userId);

        // Convert heatmap to array of {date, minutes} for the frontend chart
        var heatmapList = rawHeatmap.entrySet().stream()
            .map(e -> Map.<String, Object>of("date", e.getKey().toString(), "minutes", e.getValue()))
            .toList();

        // Build score trend from test history (last 6 data points)
        var testLabels = tests.stream().map(t -> t.get("date").toString()).toList();
        var testScores = tests.stream().map(t -> t.get("score")).toList();

        long topicsTotal    = ((Number) summary.getOrDefault("topicsTotal",    0)).longValue();
        long topicsMastered = ((Number) summary.getOrDefault("topicsMastered", 0)).longValue();
        long testsCompleted = ((Number) summary.getOrDefault("testsCompleted", 0)).longValue();
        double avgScore     = ((Number) summary.getOrDefault("avgTestScore",   0.0)).doubleValue();
        int streak          = ((Number) summary.getOrDefault("currentStreak",  0)).intValue();
        int readiness       = ((Number) summary.getOrDefault("readinessScore", 0)).intValue();

        return ResponseEntity.ok(Map.of(
            "stats", Map.of(
                "topicsCompleted", topicsMastered,
                "testsTaken",      testsCompleted,
                "avgScore",        (int) avgScore,
                "currentStreak",   streak
            ),
            "heatmap",          heatmapList,
            "readiness",        readiness,
            "weakness",         List.of(),
            "strengths",        List.of(),
            "progressChart",    Map.of("labels", testLabels, "scores", testScores),
            "activityChart",    Map.of("labels", List.of(), "minutes", List.of())
        ));
    }

    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> getSummary() {
        return ResponseEntity.ok(analyticsService.getSummary(SecurityUtil.getCurrentUserId()));
    }

    @GetMapping("/heatmap")
    public ResponseEntity<Map<LocalDate, Integer>> getHeatmap() {
        return ResponseEntity.ok(analyticsService.getHeatmap(SecurityUtil.getCurrentUserId()));
    }

    @GetMapping("/topics")
    public ResponseEntity<Map<String, String>> getTopicProgress() {
        return ResponseEntity.ok(analyticsService.getTopicProgress(SecurityUtil.getCurrentUserId()));
    }

    @GetMapping("/test-performance")
    public ResponseEntity<List<Map<String, Object>>> getTestPerformance() {
        return ResponseEntity.ok(analyticsService.getTestPerformance(SecurityUtil.getCurrentUserId()));
    }
}
