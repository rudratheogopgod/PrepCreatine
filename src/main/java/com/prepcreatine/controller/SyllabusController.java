package com.prepcreatine.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Syllabus controller — serves the exam syllabus topic list.
 * Currently returns stub data until the topic/syllabus DB is populated.
 */
@RestController
@RequestMapping("/api/syllabus")
public class SyllabusController {

    /** GET /api/syllabus — returns the syllabus topic tree. */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getSyllabus() {
        return ResponseEntity.ok(Map.of(
            "subjects", List.of(),
            "message",  "Your syllabus will appear here after completing onboarding."
        ));
    }

    /** PATCH /api/syllabus/topics/{topicId}/status — marks topic as done/skipped/in_progress. */
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
