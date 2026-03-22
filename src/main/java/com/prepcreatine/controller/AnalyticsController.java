package com.prepcreatine.controller;

import com.prepcreatine.service.AnalyticsService;
import com.prepcreatine.service.SpacedRepetitionService;
import com.prepcreatine.service.UserService;
import com.prepcreatine.util.SecurityUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
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
 * GET /api/analytics/me              — composite status for demo banner / dashboard
 * GET /api/analytics/knowledge-graph — concept mastery nodes
 */
@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

    private final AnalyticsService        analyticsService;
    private final SpacedRepetitionService spacedRep;
    private final JdbcTemplate            jdbc;
    private final UserService             userService;

    public AnalyticsController(AnalyticsService analyticsService,
                               SpacedRepetitionService spacedRep,
                               JdbcTemplate jdbc,
                               UserService userService) {
        this.analyticsService = analyticsService;
        this.spacedRep        = spacedRep;
        this.jdbc             = jdbc;
        this.userService      = userService;
    }

    /** GET /api/analytics — composite dashboard endpoint. */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getAll() {
        UUID userId = SecurityUtil.getCurrentUserId();
        Map<String, Object> summary         = analyticsService.getSummary(userId);
        Map<LocalDate, Integer> rawHeatmap  = analyticsService.getHeatmap(userId);
        Map<String, String>  topics         = analyticsService.getTopicProgress(userId);
        List<Map<String, Object>> tests     = analyticsService.getTestPerformance(userId);

        var heatmapList = rawHeatmap.entrySet().stream()
            .map(e -> Map.<String, Object>of("date", e.getKey().toString(), "minutes", e.getValue()))
            .toList();
        var testLabels = tests.stream().map(t -> t.get("date").toString()).toList();
        var testScores = tests.stream().map(t -> t.get("score")).toList();

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
            "heatmap",       heatmapList,
            "readiness",     readiness,
            "weakness",      List.of(),
            "strengths",     List.of(),
            "progressChart", Map.of("labels", testLabels, "scores", testScores),
            "activityChart", Map.of("labels", List.of(), "minutes", List.of())
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

    /**
     * GET /api/analytics/me
     * Composite status for demo banner, sidebar badges, and dashboard.
     */
    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> getMe() {
        UUID userId = SecurityUtil.getCurrentUserId();
        Map<String, Object> summary = analyticsService.getSummary(userId);
        long reviewDue = spacedRep.getDueTodayCount(userId);

        return ResponseEntity.ok(Map.of(
            "userId",          userId,
            "currentStreak",   ((Number) summary.getOrDefault("currentStreak",  0)).intValue(),
            "readinessScore",  ((Number) summary.getOrDefault("readinessScore", 0)).intValue(),
            "topicsCompleted", ((Number) summary.getOrDefault("topicsMastered", 0)).longValue(),
            "reviewDueToday",  reviewDue,
            "testsTaken",      ((Number) summary.getOrDefault("testsCompleted", 0)).longValue(),
            "averageScore",    ((Number) summary.getOrDefault("avgTestScore",   0.0)).doubleValue()
        ));
    }

    /**
     * GET /api/analytics/knowledge-graph
     * Returns concept mastery nodes for the knowledge graph visualisation.
     */
    @GetMapping("/knowledge-graph")
    public ResponseEntity<Map<String, Object>> getKnowledgeGraph() {
        UUID userId = SecurityUtil.getCurrentUserId();
        try {
            List<Map<String, Object>> nodes = jdbc.queryForList(
                """
                SELECT id::text, topic_id as "topicId", concept,
                       mastery::float as mastery, user_id::text as "userId"
                FROM concept_graph_nodes
                WHERE user_id = ?
                ORDER BY mastery DESC
                LIMIT 100
                """, userId);
            return ResponseEntity.ok(Map.of("nodes", nodes));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("nodes", List.of()));
        }
    }
}
