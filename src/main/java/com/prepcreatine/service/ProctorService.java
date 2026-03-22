package com.prepcreatine.service;

import com.prepcreatine.domain.ProctoringEvent;
import com.prepcreatine.domain.TestSession;
import com.prepcreatine.exception.ForbiddenException;
import com.prepcreatine.exception.ResourceNotFoundException;
import com.prepcreatine.repository.ProctoringEventRepository;
import com.prepcreatine.repository.TestSessionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * ProctorService — saves and retrieves proctoring events for a test session.
 * Events: gaze_away, tab_switch, face_lost. Counts are upserted (incremented).
 * Integrity rating: High (0 events), Medium (1-3), Low (4+).
 */
@Service
@Transactional
public class ProctorService {

    private static final Logger log = LoggerFactory.getLogger(ProctorService.class);

    private final TestSessionRepository     testSessionRepo;
    private final ProctoringEventRepository proctoringRepo;

    public ProctorService(TestSessionRepository testSessionRepo,
                          ProctoringEventRepository proctoringRepo) {
        this.testSessionRepo = testSessionRepo;
        this.proctoringRepo  = proctoringRepo;
    }

    /**
     * Saves proctoring events for a test session.
     * Each event type is upserted — if the session already has a row for that
     * event_type, the count is incremented (not reset).
     */
    public void saveEvents(UUID userId, UUID testSessionId,
                           List<ProctoringEventDto> events) {
        TestSession session = testSessionRepo.findById(testSessionId)
            .orElseThrow(() -> new ResourceNotFoundException("Test session not found."));
        if (!session.getUserId().equals(userId)) {
            throw new ForbiddenException("Not your test session.", userId);
        }
        for (ProctoringEventDto event : events) {
            if (event.count() > 0) {
                proctoringRepo.upsertEvent(userId, testSessionId,
                    event.eventType(), event.count());
            }
        }
        log.debug("[Proctor] Saved {} event types for session={}", events.size(), testSessionId);
    }

    /**
     * Returns a proctoring summary for a test session.
     * Used by the TestResults page to show the integrity badge.
     */
    @Transactional(readOnly = true)
    public ProctoringEventSummary getSummary(UUID userId, UUID testSessionId) {
        TestSession session = testSessionRepo.findById(testSessionId)
            .orElseThrow(() -> new ResourceNotFoundException("Test session not found."));
        if (!session.getUserId().equals(userId)) {
            throw new ForbiddenException("Not your test session.", userId);
        }

        List<ProctoringEvent> events = proctoringRepo.findByTestSessionId(testSessionId);

        int gazeAway = sumEvents(events, "gaze_away");
        int tabSwitch = sumEvents(events, "tab_switch");
        int faceLost  = sumEvents(events, "face_lost");
        int total     = gazeAway + tabSwitch + faceLost;

        String integrity = total == 0    ? "High"
                         : total <= 3    ? "Medium"
                         :                "Low";

        return new ProctoringEventSummary(gazeAway, tabSwitch, faceLost, integrity);
    }

    private int sumEvents(List<ProctoringEvent> events, String type) {
        return events.stream()
            .filter(e -> type.equals(e.getEventType()))
            .mapToInt(ProctoringEvent::getEventCount)
            .sum();
    }

    // ── Inner DTOs ────────────────────────────────────────────────────────────

    /** DTO for incoming event from frontend (JSON). */
    public record ProctoringEventDto(String eventType, int count) {}

    /** DTO for outgoing summary response. */
    public record ProctoringEventSummary(
        int gazeAwayCount,
        int tabSwitchCount,
        int faceLostCount,
        String overallIntegrity
    ) {}
}
