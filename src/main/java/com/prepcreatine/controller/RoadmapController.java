package com.prepcreatine.controller;

import com.prepcreatine.util.SecurityUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Roadmap controller — serves study roadmap for the authenticated user.
 * Currently returns stub data; AI-generated roadmaps are a planned feature.
 */
@RestController
@RequestMapping("/api/roadmap")
public class RoadmapController {

    /** GET /api/roadmap — returns the user's current roadmap (stub). */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getRoadmap() {
        return ResponseEntity.ok(Map.of(
            "topics",      List.of(),
            "generatedAt", (Object) null,
            "message",     "Roadmap generation is powered by AI. Complete onboarding to generate your first roadmap."
        ));
    }

    /** POST /api/roadmap/regenerate — queues AI regeneration. */
    @PostMapping("/regenerate")
    public ResponseEntity<Map<String, String>> regenerate() {
        return ResponseEntity.accepted()
            .body(Map.of("message", "Roadmap regeneration queued. Check back shortly."));
    }

    /** PATCH /api/roadmap/topics/{topicId}/status — marks a topic status. */
    @PatchMapping("/topics/{topicId}/status")
    public ResponseEntity<Map<String, Object>> updateTopicStatus(
            @PathVariable String topicId,
            @RequestBody Map<String, String> body) {
        String status = body.getOrDefault("status", "in_progress");
        return ResponseEntity.ok(Map.of(
            "topicId", topicId,
            "status",  status,
            "updated", true
        ));
    }
}
