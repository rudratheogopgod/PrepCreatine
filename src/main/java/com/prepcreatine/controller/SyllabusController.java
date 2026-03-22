package com.prepcreatine.controller;

import com.prepcreatine.domain.UserTopicProgress;
import com.prepcreatine.service.SpacedRepetitionService;
import com.prepcreatine.repository.UserContextRepository;
import com.prepcreatine.repository.UserTopicProgressRepository;
import com.prepcreatine.util.EvalLogger;
import com.prepcreatine.util.SecurityUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * SyllabusController — real implementation backed by user_topic_progress table.
 *
 * GET  /api/syllabus                     — user's topic progress list
 * PATCH /api/syllabus/topics/{id}/status — mark topic done/in_progress; triggers SM-2
 * GET  /api/syllabus/review-due          — topics due for spaced repetition review
 * PUT  /api/syllabus/spaced-rep/{id}     — manually trigger SM-2 review after chat review
 */
@RestController
@RequestMapping("/api/syllabus")
public class SyllabusController {

    private static final Logger log = LoggerFactory.getLogger(SyllabusController.class);

    private final UserTopicProgressRepository progressRepo;
    private final UserContextRepository       contextRepo;
    private final SpacedRepetitionService     spacedRep;

    public SyllabusController(UserTopicProgressRepository progressRepo,
                              UserContextRepository contextRepo,
                              SpacedRepetitionService spacedRep) {
        this.progressRepo = progressRepo;
        this.contextRepo  = contextRepo;
        this.spacedRep    = spacedRep;
    }

    /**
     * GET /api/syllabus
     * Returns all topic progress rows for the current user.
     * Frontend uses this to render the syllabus tracker / roadmap.
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getSyllabus() {
        UUID userId = SecurityUtil.getCurrentUserId();
        List<UserTopicProgress> progress = progressRepo.findByUserId(userId);

        List<Map<String, Object>> topics = progress.stream()
            .map(this::toTopicMap)
            .collect(Collectors.toList());

        long done       = progress.stream().filter(p -> "done".equals(p.getStatus())).count();
        long inProgress = progress.stream().filter(p -> "in_progress".equals(p.getStatus())).count();
        long notStarted = progress.stream().filter(p -> "not_started".equals(p.getStatus())).count();

        return ResponseEntity.ok(Map.of(
            "topics",      topics,
            "total",       progress.size(),
            "done",        done,
            "inProgress",  inProgress,
            "notStarted",  notStarted
        ));
    }

    /**
     * PATCH /api/syllabus/topics/{topicId}/status
     * Updates a topic's status. If marked 'done', initialises SM-2 scheduling.
     * Body: { status: "done" | "in_progress" | "not_started", examId: String, subjectId: String }
     */
    @PatchMapping("/topics/{topicId}/status")
    public ResponseEntity<Map<String, Object>> updateTopicStatus(
            @PathVariable String topicId,
            @RequestBody Map<String, String> body) {

        UUID userId   = SecurityUtil.getCurrentUserId();
        String status = body.getOrDefault("status", "in_progress");
        String examId    = body.getOrDefault("examId", "jee");
        String subjectId = body.getOrDefault("subjectId", "general");

        log.info("[SyllabusController] PATCH topic status: userId={}, topicId={}, newStatus={}",
            userId, topicId, status);
        if ("done".equals(status)) {
            EvalLogger.step(log, "MODULE-1 SM2", "Topic marked done \u2192 triggering SM-2 scheduling: topicId=" + topicId);
        }

        UserTopicProgress p = progressRepo.findByUserIdAndTopicId(userId, topicId)
            .orElseGet(() -> {
                UserTopicProgress np = new UserTopicProgress();
                np.setUserId(userId);
                np.setTopicId(topicId);
                np.setExamId(examId);
                np.setSubjectId(subjectId);
                return np;
            });

        p.setStatus(status);

        // If marking done for the first time, initialise SM-2
        if ("done".equals(status) && p.getNextReviewDate() == null) {
            spacedRep.initializeForTopic(userId, topicId, examId, subjectId);
        }

        progressRepo.save(p);

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("topicId", topicId);
        resp.put("status",  status);
        resp.put("updated", true);
        if ("done".equals(status)) {
            resp.put("nextReviewDate", p.getNextReviewDate() != null
                ? p.getNextReviewDate().toString()
                : LocalDate.now().plusDays(1).toString());
            EvalLogger.success(log, "MODULE-1 SYLLABUS",
                String.format("Topic status updated: topicId=%s \u2192 %s", topicId, status));
            if (p.getNextReviewDate() != null) {
                EvalLogger.result(log, "MODULE-1 SM2", "Next review date scheduled", p.getNextReviewDate());
            }
        } else {
            EvalLogger.success(log, "MODULE-1 SYLLABUS",
                String.format("Topic status updated: topicId=%s \u2192 %s", topicId, status));
        }
        return ResponseEntity.ok(resp);
    }

    /**
     * GET /api/syllabus/review-due
     * Returns topics due for spaced repetition review today or earlier.
     * Response: { dueCount: int, topics: [ { topicId, status, nextReviewDate, ... } ] }
     * Used by ReviewDueWidget on the home page and sidebar badge.
     */
    @GetMapping("/review-due")
    public ResponseEntity<Map<String, Object>> getReviewDue() {
        UUID userId = SecurityUtil.getCurrentUserId();
        log.info("[SyllabusController] GET /api/syllabus/review-due: userId={}", userId);
        List<UserTopicProgress> due = spacedRep.getDueToday(userId);

        List<Map<String, Object>> topics = due.stream()
            .map(p -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("topicId",         p.getTopicId());
                m.put("subjectId",       p.getSubjectId());
                m.put("status",          p.getStatus());
                m.put("nextReviewDate",  p.getNextReviewDate() != null ? p.getNextReviewDate().toString() : null);
                m.put("intervalDays",    p.getIntervalDays());
                m.put("daysOverdue",     p.getNextReviewDate() != null
                    ? LocalDate.now().toEpochDay() - p.getNextReviewDate().toEpochDay() : 0);
                m.put("easinessFactor",  p.getEasinessFactor());
                m.put("repetitionCount", p.getRepetitionCount());
                return m;
            })
            .collect(Collectors.toList());

        EvalLogger.result(log, "MODULE-1 REVIEW-DUE", "dueCount", topics.size());
        EvalLogger.success(log, "MODULE-1 REVIEW-DUE",
            "Review-due endpoint returned " + topics.size() + " topics");

        return ResponseEntity.ok(Map.of(
            "dueCount", topics.size(),
            "topics",   topics
        ));
    }

    /**
     * PUT /api/syllabus/spaced-rep/{topicId}
     * Manually applies an SM-2 review — called after a student reviews a topic via chat.
     * Body: { quality: int (0-5), examId: String }
     * Response: { topicId, nextReviewDate, intervalDays }
     */
    @PutMapping("/spaced-rep/{topicId}")
    public ResponseEntity<Map<String, Object>> updateSpacedRep(
            @PathVariable String topicId,
            @RequestBody Map<String, Object> body) {

        UUID userId  = SecurityUtil.getCurrentUserId();
        int quality  = body.containsKey("quality") ? ((Number) body.get("quality")).intValue() : 3;

        UserTopicProgress updated = spacedRep.applyReview(userId, topicId, quality);

        return ResponseEntity.ok(Map.of(
            "topicId",       topicId,
            "nextReviewDate", updated.getNextReviewDate() != null
                ? updated.getNextReviewDate().toString() : null,
            "intervalDays",  updated.getIntervalDays(),
            "easinessFactor", updated.getEasinessFactor()
        ));
    }

    // ── Helper ─────────────────────────────────────────────────────────────────

    private Map<String, Object> toTopicMap(UserTopicProgress p) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("topicId",         p.getTopicId());
        m.put("examId",          p.getExamId());
        m.put("subjectId",       p.getSubjectId());
        m.put("status",          p.getStatus());
        m.put("timeSpentMins",   p.getTimeSpentMins());
        m.put("lastScore",       p.getLastScore());
        m.put("testAttempts",    p.getTestAttempts());
        m.put("nextReviewDate",  p.getNextReviewDate() != null ? p.getNextReviewDate().toString() : null);
        m.put("intervalDays",    p.getIntervalDays());
        m.put("easinessFactor",  p.getEasinessFactor());
        m.put("repetitionCount", p.getRepetitionCount());
        return m;
    }
}
