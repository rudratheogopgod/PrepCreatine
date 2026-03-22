package com.prepcreatine.demo;

import com.prepcreatine.config.DemoModeConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Demo helper endpoints per BSDD v2.1 §6A.
 *
 * GET /api/demo/status — returns demo state info for judges
 * GET /api/demo/reset  — resets mutable demo data between demo runs
 *
 * Only registered in Spring context when app.demo-mode=true.
 */
@RestController
@RequestMapping("/api/demo")
@ConditionalOnProperty(name = "app.demo-mode", havingValue = "true")
public class DemoController {

    private static final Logger log = LoggerFactory.getLogger(DemoController.class);

    private final JdbcTemplate jdbc;
    private final CacheManager cacheManager;
    private final DemoUserSeeder seeder;

    public DemoController(JdbcTemplate jdbc,
                          CacheManager cacheManager,
                          DemoUserSeeder seeder) {
        this.jdbc = jdbc;
        this.cacheManager = cacheManager;
        this.seeder = seeder;
    }

    /**
     * GET /api/demo/status — returns current demo state and available features.
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> status() {
        log.info("[Demo] /status called");

        // Fetch live stats from DB
        String userId = DemoModeConfig.DEMO_USER_ID.toString();

        Integer topicsDone = jdbc.queryForObject(
            "SELECT COUNT(*) FROM user_topic_progress WHERE user_id=?::uuid AND status='done'",
            Integer.class, userId);

        Integer testsTaken = jdbc.queryForObject(
            "SELECT COUNT(*) FROM test_sessions WHERE user_id=?::uuid AND status='submitted'",
            Integer.class, userId);

        Integer reviewDue = jdbc.queryForObject(
            "SELECT COUNT(*) FROM user_topic_progress WHERE user_id=?::uuid AND next_review_date <= CURRENT_DATE",
            Integer.class, userId);

        Integer streak = jdbc.queryForObject(
            "SELECT current_streak FROM users WHERE id=?::uuid",
            Integer.class, userId);

        Integer readiness = jdbc.queryForObject(
            "SELECT readiness_score FROM users WHERE id=?::uuid",
            Integer.class, userId);

        LocalDate examDate = jdbc.queryForObject(
            "SELECT exam_date FROM users WHERE id=?::uuid",
            LocalDate.class, userId);

        Map<String, Object> demoUser = Map.of(
            "userId",           DemoModeConfig.DEMO_USER_ID.toString(),
            "fullName",         "Arjun Sharma",
            "examType",         "jee",
            "examDate",         examDate != null ? examDate.toString() : "",
            "currentStreak",    streak != null ? streak : 14,
            "topicsCompleted",  topicsDone != null ? topicsDone : 45,
            "reviewDueToday",   reviewDue != null ? reviewDue : 6,
            "readinessScore",   readiness != null ? readiness : 61
        );

        Map<String, String> demoScript = Map.of(
            "step1", "Home screen — review due widget shows 6 topics",
            "step2", "Today's Plan — 5-session daily plan with motivation",
            "step3", "Chat — ask about thermodynamics entropy",
            "step4", "Syllabus — mark jee-physics-semiconductors as done → SM-2",
            "step5", "Take targeted drill — Session 36 (Organic GOC, 3 Qs)",
            "step6", "Analytics — knowledge graph, readiness score 61",
            "step7", "Notes — paste YouTube URL for a Vedantu/PW video",
            "step8", "Community — show mentor answer + Reddit pulse",
            "step9", "Game mode — start a thermodynamics game"
        );

        return ResponseEntity.ok(Map.of(
            "demoMode", true,
            "demoUser", demoUser,
            "availableFeatures", List.of(
                "Spaced Repetition (6 topics due today)",
                "Daily Study Plan (pre-generated)",
                "AI Tutor Chat (Thermodynamics + Integration notes loaded)",
                "Mock Test (Full mock + Targeted drills available)",
                "Knowledge Graph (15 concepts across 4 topics)",
                "YouTube Video Import",
                "Community Q&A (1 thread with mentor answer)",
                "Mentor Dashboard (Dr. Priya Mehta linked)",
                "Analytics Dashboard (14-day streak, 6 tests)"
            ),
            "demoScript", demoScript
        ));
    }

    /**
     * GET /api/demo/reset — resets all mutable demo data back to seeded state.
     * Called by the Reset button in DemoBanner on the frontend.
     */
    @GetMapping("/reset")
    public ResponseEntity<Map<String, String>> reset() {
        log.info("[Demo] Reset triggered");

        String userId = DemoModeConfig.DEMO_USER_ID.toString();

        // 1. Delete test sessions created after the 8 seeded ones
        jdbc.update("""
            DELETE FROM test_sessions
            WHERE user_id=?::uuid
              AND id NOT IN (
                '00000000-0000-0000-0000-000000000030'::uuid,
                '00000000-0000-0000-0000-000000000031'::uuid,
                '00000000-0000-0000-0000-000000000032'::uuid,
                '00000000-0000-0000-0000-000000000033'::uuid,
                '00000000-0000-0000-0000-000000000034'::uuid,
                '00000000-0000-0000-0000-000000000035'::uuid,
                '00000000-0000-0000-0000-000000000036'::uuid,
                '00000000-0000-0000-0000-000000000037'::uuid
              )
            """, userId);

        // 2. Delete concept graph nodes not in seeded 15
        jdbc.update("""
            DELETE FROM concept_graph_nodes
            WHERE user_id=?::uuid
              AND topic_id NOT IN (
                'jee-physics-thermodynamics','jee-chemistry-organic-goc',
                'jee-math-integration','jee-physics-kinematics'
              )
            """, userId);

        // 3. Delete sources not in seeded 2
        jdbc.update("""
            DELETE FROM sources
            WHERE user_id=?::uuid
              AND id NOT IN (
                '00000000-0000-0000-0000-000000000020'::uuid,
                '00000000-0000-0000-0000-000000000021'::uuid
              )
            """, userId);

        // 4. Delete notifications not in seeded 4 (keep only the first 4)
        jdbc.update("""
            DELETE FROM notifications
            WHERE user_id=?::uuid
              AND id NOT IN (
                SELECT id FROM notifications
                WHERE user_id=?::uuid
                ORDER BY created_at
                LIMIT 4
              )
            """, userId, userId);

        // 5. Re-seed topic progress back to seeded state
        try { seeder.seedSyllabusProgress(); } catch (Exception e) {
            log.warn("[Demo] Reset: syllabusProgress re-seed failed: {}", e.getMessage());
        }

        // 6. Reset daily plan to seeded version
        try { seeder.seedDailyPlan(); } catch (Exception e) {
            log.warn("[Demo] Reset: dailyPlan re-seed failed: {}", e.getMessage());
        }

        // 7. Invalidate all Caffeine caches
        cacheManager.getCacheNames().forEach(name -> {
            var cache = cacheManager.getCache(name);
            if (cache != null) cache.clear();
        });

        log.info("[Demo] Reset complete");
        return ResponseEntity.ok(Map.of("message", "Demo data reset successfully."));
    }
}
