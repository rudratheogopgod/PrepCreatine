package com.prepcreatine.controller;

import com.prepcreatine.config.DemoModeConfig;
import com.prepcreatine.util.SecurityUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * DemoController — demo mode management endpoints.
 *
 * GET  /api/demo/status  — returns demo mode info (for the demo banner)
 * POST /api/demo/reset   — resets the current demo user's data back to defaults
 *
 * These endpoints are always accessible (demo user or real user).
 * POST /api/demo/reset only clears the current user's progress data.
 */
@RestController
@RequestMapping("/api/demo")
public class DemoController {

    private static final Logger log = LoggerFactory.getLogger(DemoController.class);

    private final DemoModeConfig demoConfig;
    private final JdbcTemplate   jdbc;

    public DemoController(DemoModeConfig demoConfig, JdbcTemplate jdbc) {
        this.demoConfig = demoConfig;
        this.jdbc       = jdbc;
    }

    /**
     * GET /api/demo/status
     * Returns whether demo mode is active and the current demo user ID.
     * Frontend uses this to show/hide the demo mode banner.
     *
     * Response:
     * {
     *   "isDemoMode": true,
     *   "demoUserId": "uuid",
     *   "message": "Demo mode is active. Data resets daily."
     * }
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        try {
            UUID currentUserId = SecurityUtil.getCurrentUserId();
            UUID demoUserId    = DemoModeConfig.DEMO_USER_ID;
            boolean isDemoUser = demoConfig.isDemoMode() && demoUserId.equals(currentUserId);

            return ResponseEntity.ok(Map.of(
                "isDemoMode", isDemoUser,
                "demoUserId", demoUserId.toString(),
                "message",    isDemoUser
                              ? "Demo mode active. Explore freely — data resets daily."
                              : "Live mode"
            ));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of(
                "isDemoMode", false,
                "demoUserId", "",
                "message",    "Not authenticated"
            ));
        }
    }

    /**
     * POST /api/demo/reset
     * Resets the current user's progress data to the demo baseline.
     * Only clears: user_topic_progress, test_sessions, test_answers, daily_plans, proctoring_events.
     * Does NOT delete the user account, uploaded sources, or notifications.
     *
     * Returns 200 with a message; the frontend should navigate back to the home screen.
     */
    @PostMapping("/reset")
    public ResponseEntity<Map<String, Object>> resetDemoData() {
        UUID userId = SecurityUtil.getCurrentUserId();
        log.info("[Demo] Resetting data for userId={}", userId);

        try {
            // Reset test answers first (foreign key to test_sessions)
            jdbc.update("DELETE FROM test_answers WHERE test_session_id IN "
                + "(SELECT id FROM test_sessions WHERE user_id = ?)", userId);
            // Reset test sessions
            jdbc.update("DELETE FROM test_sessions WHERE user_id = ?", userId);
            // Reset topic progress (SM-2 resets too)
            jdbc.update("DELETE FROM user_topic_progress WHERE user_id = ?", userId);
            // Reset daily plans
            jdbc.update("DELETE FROM daily_plans WHERE user_id = ?", userId);
            // Reset proctoring events
            jdbc.update("DELETE FROM proctoring_events WHERE user_id = ? "
                + "OR test_session_id IN (SELECT id FROM test_sessions WHERE user_id = ?)", userId, userId);
            // Reset notifications
            jdbc.update("DELETE FROM notifications WHERE user_id = ?", userId);

            log.info("[Demo] Reset complete for userId={}", userId);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Your progress has been reset. Start fresh!"
            ));
        } catch (Exception e) {
            log.error("[Demo] Reset failed for userId={}: {}", userId, e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "Reset failed. Please try again."
            ));
        }
    }
}
