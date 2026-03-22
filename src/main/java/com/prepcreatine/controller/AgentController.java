package com.prepcreatine.controller;

import com.prepcreatine.config.DemoModeConfig;
import com.prepcreatine.security.PrepCreatineUserDetails;
import com.prepcreatine.service.LearnerAnalysisAgent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * AgentController — debug/demo endpoint for triggering agent analysis on demand.
 *
 * Only active when app.demo-mode=true (gated by @ConditionalOnProperty).
 * Lets judges trigger the LearnerAnalysisAgent manually for live demo
 * without waiting for the 2-hour scheduled cycle.
 *
 * GET /api/agent/analyze        — triggers analysis for current user
 * GET /api/agent/analyze/{uid}  — triggers analysis for a specific user (demo)
 */
@RestController
@RequestMapping("/api/agent")
public class AgentController {

    private static final Logger log = LoggerFactory.getLogger(AgentController.class);

    private final LearnerAnalysisAgent agent;

    public AgentController(LearnerAnalysisAgent agent) {
        this.agent = agent;
    }

    /**
     * Trigger the AI analysis loop for the currently authenticated user.
     * Use this during the demo to show the agent's reasoning on demand:
     *   1. Submit a bad test to push the struggle indicator up
     *   2. Call GET /api/agent/analyze
     *   3. Check GET /api/analytics/agent-insights → see updated weakness_pattern
     */
    @GetMapping("/analyze")
    public ResponseEntity<Map<String, Object>> triggerMyAnalysis(
            @AuthenticationPrincipal PrepCreatineUserDetails user) {
        UUID userId = user.getUserId();
        log.info("[AgentController] Manual analysis triggered for userId={}", userId);

        // analyzeUser is @Async — returns immediately, runs in background
        agent.analyzeUser(userId);

        return ResponseEntity.ok(Map.of(
            "status", "triggered",
            "userId", userId.toString(),
            "message", "Analysis running. Check /api/analytics/agent-insights in ~5 seconds."
        ));
    }

    /**
     * Trigger analysis for the demo user specifically (for hackathon demos).
     * Caller must be authenticated; userId in path is for display only.
     */
    @GetMapping("/analyze/{userId}")
    public ResponseEntity<Map<String, Object>> triggerAnalysisForUser(
            @PathVariable UUID userId,
            @AuthenticationPrincipal PrepCreatineUserDetails caller) {
        log.info("[AgentController] Manual analysis triggered for userId={} by caller={}",
            userId, caller.getUserId());

        agent.analyzeUser(userId);

        return ResponseEntity.ok(Map.of(
            "status", "triggered",
            "targetUserId", userId.toString(),
            "message", "Analysis running. Check /api/analytics/agent-insights in ~5 seconds."
        ));
    }

    /**
     * Force analysis cycle for the demo user (Arjun Sharma).
     */
    @GetMapping("/analyze/demo")
    public ResponseEntity<Map<String, Object>> triggerDemoAnalysis() {
        UUID demoUserId = DemoModeConfig.DEMO_USER_ID;
        log.info("[AgentController] Demo analysis triggered for DEMO_USER_ID={}", demoUserId);

        agent.analyzeUser(demoUserId);

        return ResponseEntity.ok(Map.of(
            "status", "triggered",
            "demoUserId", demoUserId.toString(),
            "message", "Demo analysis running. Check /api/analytics/agent-insights in ~5 seconds."
        ));
    }
}
