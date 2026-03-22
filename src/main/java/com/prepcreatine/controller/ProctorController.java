package com.prepcreatine.controller;

import com.prepcreatine.service.ProctorService;
import com.prepcreatine.service.ProctorService.ProctoringEventDto;
import com.prepcreatine.service.ProctorService.ProctoringEventSummary;
import com.prepcreatine.util.SecurityUtil;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * ProctorController — endpoints for webcam proctoring during tests.
 *
 * POST /api/proctor/events           — save gaze/tab/face events at test end
 * GET  /api/proctor/summary/{id}     — get integrity summary for a session
 */
@RestController
@RequestMapping("/api/proctor")
public class ProctorController {

    private final ProctorService proctorService;

    public ProctorController(ProctorService proctorService) {
        this.proctorService = proctorService;
    }

    /**
     * POST /api/proctor/events
     * Saves proctoring event counts for a test session.
     * Called from the frontend when the test is submitted or session ends.
     * Body: { testSessionId: UUID, events: [{ eventType: String, count: int }] }
     */
    @PostMapping("/events")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void saveEvents(@RequestBody Map<String, Object> body) {
        UUID userId = SecurityUtil.getCurrentUserId();
        UUID testSessionId = UUID.fromString((String) body.get("testSessionId"));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rawEvents = (List<Map<String, Object>>) body.get("events");

        List<ProctoringEventDto> events = rawEvents == null ? List.of() :
            rawEvents.stream()
                .map(e -> new ProctoringEventDto(
                    (String) e.get("eventType"),
                    ((Number) e.getOrDefault("count", 0)).intValue()
                ))
                .toList();

        proctorService.saveEvents(userId, testSessionId, events);
    }

    /**
     * GET /api/proctor/summary/{testSessionId}
     * Returns proctoring summary for the test results page.
     * Response: { gazeAwayCount, tabSwitchCount, faceLostCount, overallIntegrity }
     */
    @GetMapping("/summary/{testSessionId}")
    public ResponseEntity<ProctoringEventSummary> getSummary(
            @PathVariable UUID testSessionId) {
        UUID userId = SecurityUtil.getCurrentUserId();
        ProctoringEventSummary summary = proctorService.getSummary(userId, testSessionId);
        return ResponseEntity.ok(summary);
    }
}
