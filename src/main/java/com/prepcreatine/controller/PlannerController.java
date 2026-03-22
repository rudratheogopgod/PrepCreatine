package com.prepcreatine.controller;

import com.prepcreatine.service.StudyPlannerService;
import com.prepcreatine.util.SecurityUtil;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * PlannerController — autonomous daily study plan endpoints.
 *
 * GET  /api/planner/today                — today's study plan (generated or cached)
 * POST /api/planner/today/session-complete — mark a session done, apply SM-2 if review
 * GET  /api/planner/week                 — 7-day overview
 */
@RestController
@RequestMapping("/api/planner")
public class PlannerController {

    private final StudyPlannerService plannerService;

    public PlannerController(StudyPlannerService plannerService) {
        this.plannerService = plannerService;
    }

    /**
     * GET /api/planner/today
     * Returns today's personalised study plan. Generates via Gemini if not cached.
     * Demo: returns the pre-seeded daily_plan for DEMO_USER_ID instantly.
     */
    @GetMapping("/today")
    public ResponseEntity<Map<String, Object>> getTodayPlan() {
        UUID userId = SecurityUtil.getCurrentUserId();
        Map<String, Object> plan = plannerService.getTodayPlan(userId);
        return ResponseEntity.ok(plan);
    }

    /**
     * POST /api/planner/today/session-complete
     * Marks a session in today's plan as complete.
     * For review sessions: also applies SM-2 quality=3.
     * Body: { topicId: String, sessionType: String, actualMins: int }
     */
    @PostMapping("/today/session-complete")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markSessionComplete(@RequestBody Map<String, Object> body) {
        UUID userId       = SecurityUtil.getCurrentUserId();
        String topicId    = (String) body.get("topicId");
        String type       = (String) body.getOrDefault("sessionType", "review");
        int    actualMins = body.containsKey("actualMins")
                           ? ((Number) body.get("actualMins")).intValue() : 0;
        plannerService.markSessionComplete(userId, topicId, type, actualMins);
    }

    /**
     * GET /api/planner/week
     * Returns 7-day plan overview (has plan, total minutes, session count).
     */
    @GetMapping("/week")
    public ResponseEntity<Map<String, Object>> getWeekOverview() {
        return ResponseEntity.ok(plannerService.getWeekOverview(SecurityUtil.getCurrentUserId()));
    }
}
