package com.prepcreatine.controller;

import com.prepcreatine.dto.response.*;
import com.prepcreatine.service.MentorService;
import com.prepcreatine.util.SecurityUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Mentor-specific endpoints per BSDD §16.
 * All endpoints require MENTOR role (enforced in SecurityConfig).
 *
 * GET    /api/mentor/students
 * GET    /api/mentor/students/{studentId}
 * GET    /api/mentor/students/{studentId}/note
 * PUT    /api/mentor/students/{studentId}/note
 * DELETE /api/mentor/students/{studentId}
 */
@RestController
@RequestMapping("/api/mentor")
public class MentorController {

    private final MentorService mentorService;

    public MentorController(MentorService mentorService) {
        this.mentorService = mentorService;
    }

    @GetMapping("/students")
    public ResponseEntity<List<UserSummaryResponse>> listStudents() {
        return ResponseEntity.ok(mentorService.listStudents(SecurityUtil.getCurrentUserId()));
    }

    @GetMapping("/students/{studentId}")
    public ResponseEntity<UserResponse> getStudent(@PathVariable UUID studentId) {
        return ResponseEntity.ok(mentorService.getStudent(SecurityUtil.getCurrentUserId(), studentId));
    }

    @GetMapping("/students/{studentId}/note")
    public ResponseEntity<Map<String, String>> getNote(@PathVariable UUID studentId) {
        String note = mentorService.getNote(SecurityUtil.getCurrentUserId(), studentId);
        return ResponseEntity.ok(Map.of("note", note));
    }

    @PutMapping("/students/{studentId}/note")
    public ResponseEntity<Map<String, String>> upsertNote(
        @PathVariable UUID studentId,
        @RequestBody Map<String, String> body
    ) {
        String content = body.getOrDefault("note", "");
        String saved   = mentorService.upsertNote(SecurityUtil.getCurrentUserId(), studentId, content);
        return ResponseEntity.ok(Map.of("note", saved));
    }

    @DeleteMapping("/students/{studentId}")
    public ResponseEntity<Map<String, String>> removeStudent(@PathVariable UUID studentId) {
        mentorService.removeStudent(SecurityUtil.getCurrentUserId(), studentId);
        return ResponseEntity.ok(Map.of("message", "Student removed from your list."));
    }
}
