package com.prepcreatine.controller;

import com.prepcreatine.dto.request.*;
import com.prepcreatine.dto.response.UserResponse;
import com.prepcreatine.service.*;
import com.prepcreatine.util.SecurityUtil;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * User / profile endpoints.
 *
 * GET    /api/me
 * PATCH  /api/me
 * DELETE /api/me
 * GET    /api/profile/{shareToken}          — public profile (no auth required)
 * POST   /api/onboarding/complete           — Path A
 * POST   /api/onboarding/quick              — Path C (speed_run)
 */
@RestController
public class UserController {

    private final UserService       userService;
    private final OnboardingService onboardingService;
    private final AnalyticsService  analyticsService;

    public UserController(UserService userService,
                          OnboardingService onboardingService,
                          AnalyticsService analyticsService) {
        this.userService       = userService;
        this.onboardingService = onboardingService;
        this.analyticsService  = analyticsService;
    }

    @GetMapping("/api/me")
    public ResponseEntity<UserResponse> getMe() {
        return ResponseEntity.ok(userService.getMe(SecurityUtil.getCurrentUserId()));
    }

    @PatchMapping("/api/me")
    public ResponseEntity<UserResponse> updateProfile(@Valid @RequestBody UpdateProfileRequest req) {
        return ResponseEntity.ok(userService.updateProfile(SecurityUtil.getCurrentUserId(), req));
    }

    @DeleteMapping("/api/me")
    public ResponseEntity<Map<String, String>> deleteAccount() {
        userService.softDeleteAccount(SecurityUtil.getCurrentUserId());
        return ResponseEntity.ok(Map.of("message", "Account deactivated successfully."));
    }

    @GetMapping("/api/profile/{shareToken}")
    public ResponseEntity<UserResponse> getPublicProfile(@PathVariable String shareToken) {
        return ResponseEntity.ok(userService.getPublicProfile(shareToken));
    }

    @PostMapping("/api/onboarding/complete")
    public ResponseEntity<UserResponse> onboardingComplete(@Valid @RequestBody OnboardingCompleteRequest req) {
        return ResponseEntity.ok(onboardingService.complete(SecurityUtil.getCurrentUserId(), req));
    }

    @PostMapping("/api/onboarding/quick")
    public ResponseEntity<UserResponse> onboardingQuick(@Valid @RequestBody OnboardingQuickRequest req) {
        return ResponseEntity.ok(onboardingService.completeQuick(SecurityUtil.getCurrentUserId(), req));
    }

    /**
     * GET /api/me/context — dashboard summary used by the Home page.
     * Returns topics progress, streak, study minutes today, and readiness score.
     */
    @GetMapping("/api/me/context")
    public ResponseEntity<Map<String, Object>> getDashboardContext() {
        UUID userId = SecurityUtil.getCurrentUserId();
        Map<String, Object> summary = analyticsService.getSummary(userId);
        // Map to the shape the Home page expects
        long topicsTotal    = ((Number) summary.getOrDefault("topicsTotal",    0)).longValue();
        long topicsMastered = ((Number) summary.getOrDefault("topicsMastered", 0)).longValue();
        return ResponseEntity.ok(Map.of(
            "topicsCompleted",  topicsMastered,
            "totalTopics",      topicsTotal,
            "streakDays",       summary.getOrDefault("currentStreak", 0),
            "studyMinutesToday", 0,  // session-level tracking TBD
            "recentActivity",   java.util.List.of()
        ));
    }
}
