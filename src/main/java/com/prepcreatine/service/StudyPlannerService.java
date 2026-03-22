package com.prepcreatine.service;

import com.prepcreatine.domain.LearnerProfile;
import com.prepcreatine.domain.UserContext;
import com.prepcreatine.domain.UserTopicProgress;
import com.prepcreatine.repository.LearnerProfileRepository;
import com.prepcreatine.repository.UserContextRepository;
import com.prepcreatine.repository.UserTopicProgressRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * StudyPlannerService — generates and returns today's personalised study plan.
 * Also implements the autonomous agent replan loop (Component D).
 */
@Service
@Transactional
public class StudyPlannerService {

    private static final Logger log = LoggerFactory.getLogger(StudyPlannerService.class);

    private final JdbcTemplate               jdbc;
    private final UserContextRepository      contextRepo;
    private final UserTopicProgressRepository progressRepo;
    private final SpacedRepetitionService    spacedRep;
    private final GeminiService              gemini;
    private final ObjectMapper               om;
    private final LearnerProfileRepository   profileRepo;
    private final NotificationService        notificationService;

    public StudyPlannerService(JdbcTemplate jdbc,
                               UserContextRepository contextRepo,
                               UserTopicProgressRepository progressRepo,
                               SpacedRepetitionService spacedRep,
                               GeminiService gemini,
                               ObjectMapper om,
                               LearnerProfileRepository profileRepo,
                               NotificationService notificationService) {
        this.jdbc                = jdbc;
        this.contextRepo         = contextRepo;
        this.progressRepo        = progressRepo;
        this.spacedRep           = spacedRep;
        this.gemini              = gemini;
        this.om                  = om;
        this.profileRepo         = profileRepo;
        this.notificationService = notificationService;
    }


    /**
     * Returns today's study plan. Cached in daily_plans table.
     * If no plan exists yet, generates one via Gemini.
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getTodayPlan(UUID userId) {
        // 1. Try cache
        Optional<Map<String, Object>> cached = loadCachedPlan(userId, LocalDate.now());
        if (cached.isPresent()) {
            log.debug("[Planner] Cache hit for userId={}", userId);
            return cached.get();
        }

        // 2. Load user context
        UserContext ctx = contextRepo.findByUserId(userId).orElse(null);
        String examId  = ctx != null && ctx.getExamType() != null ? ctx.getExamType() : "jee";
        Integer rawGoal = ctx != null ? ctx.getDailyGoalMins() : null;
        int dailyGoal  = rawGoal != null ? rawGoal : 120;


        // 3. Load review-due topics
        List<UserTopicProgress> dueToday = spacedRep.getDueToday(userId);

        // 4. Load weak topics from context
        String[] weakTopics = ctx != null && ctx.getWeakTopics() != null ? ctx.getWeakTopics() : new String[0];

        // 5. Build plan
        Map<String, Object> plan = buildPlan(userId, examId, dailyGoal, dueToday, weakTopics, ctx);

        // 6. Cache to daily_plans
        savePlan(userId, LocalDate.now(), plan);

        return plan;
    }

    /**
     * Marks a session in today's plan as complete. Updates SM-2 if it's a review.
     */
    public void markSessionComplete(UUID userId, String topicId, String sessionType, int actualMins) {
        if ("review".equals(sessionType) && topicId != null) {
            spacedRep.applyReview(userId, topicId, 3); // quality 3 = good review
        }
        // Update sessions_json in daily_plans to mark this topicId complete
        try {
            String update = """
                UPDATE daily_plans
                SET sessions_json = (
                    SELECT jsonb_agg(
                        CASE WHEN session->>'topicId' = ? THEN session || '{"isCompleted":true}'::jsonb
                             ELSE session END
                    )
                    FROM jsonb_array_elements(sessions_json) AS session
                )
                WHERE user_id = ? AND plan_date = CURRENT_DATE
                """;
            jdbc.update(update, topicId, userId);
        } catch (Exception e) {
            log.warn("[Planner] Could not mark session complete: {}", e.getMessage());
        }
    }

    /**
     * Returns a simple 7-day overview (for the /planner/week view).
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getWeekOverview(UUID userId) {
        try {
            List<Map<String, Object>> days = new ArrayList<>();
            LocalDate today = LocalDate.now();
            for (int i = 0; i < 7; i++) {
                LocalDate date = today.minusDays(6 - i);
                Optional<Map<String, Object>> plan = loadCachedPlan(userId, date);
                Map<String, Object> day = new LinkedHashMap<>();
                day.put("date", date.toString());
                day.put("isToday", date.equals(today));
                if (plan.isPresent()) {
                    Object sessions = plan.get().get("sessions");
                    int sessionCount = sessions instanceof List ? ((List<?>) sessions).size() : 0;
                    day.put("hasPlan", true);
                    day.put("totalMinutes", plan.get().getOrDefault("totalMinutes", 0));
                    day.put("sessionCount", sessionCount);
                } else {
                    day.put("hasPlan", false);
                    day.put("totalMinutes", 0);
                    day.put("sessionCount", 0);
                }
                days.add(day);
            }
            return Map.of("week", days);
        } catch (Exception e) {
            log.warn("[Planner] Week overview error: {}", e.getMessage());
            return Map.of("week", List.of());
        }
    }

    // ── Private helpers ────────────────────────────────────────────────────────

    private Map<String, Object> buildPlan(UUID userId, String examId, int dailyGoalMins,
                                          List<UserTopicProgress> dueToday,
                                          String[] weakTopics, UserContext ctx) {
        // Build prompt
        String dueStr = dueToday.stream()
            .limit(3)
            .map(d -> "- " + humanTopicName(d.getTopicId()) + " (overdue " +
                      ChronoUnit.DAYS.between(d.getNextReviewDate(), LocalDate.now()) + " days)")
            .collect(Collectors.joining("\n"));

        String weakStr = weakTopics.length > 0 ? String.join(", ", weakTopics) : "none identified";
        LocalDate examDate = ctx != null ? ctx.getExamDate() : null;
        long daysLeft = examDate != null ? ChronoUnit.DAYS.between(LocalDate.now(), examDate) : -1;

        String prompt = """
            You are an autonomous study planning agent for %s exam preparation.
            Generate a concrete study plan for TODAY only.

            STUDENT PROFILE:
            - Daily study goal: %d minutes
            - Exam date: %s (%s days left)
            - Weak topics: %s
            - Topics due for spaced repetition review today:
            %s

            PRIORITY: 1) Review-due topics first, 2) Weak topics, 3) New topics

            Return ONLY valid JSON. No markdown. No preamble. Format:
            {
              "planDate": "%s",
              "totalMinutes": %d,
              "sessions": [
                {
                  "type": "review",
                  "topicId": "jee-physics-work-energy",
                  "topicName": "Work, Energy and Power",
                  "subjectId": "physics",
                  "subjectName": "Physics",
                  "durationMins": 20,
                  "reason": "Due for spaced repetition review"
                }
              ],
              "motivationMessage": "Encouraging 1-sentence message mentioning the exam"
            }
            Session types: review (spaced rep), practice (test drill), new (first time).
            Total durationMins MUST equal %d exactly. Each session: 15-60 mins.
            """
            .formatted(examId.toUpperCase(), dailyGoalMins,
                       examDate != null ? examDate : "not set",
                       daysLeft > 0 ? daysLeft : "unknown",
                       weakStr, dueStr.isEmpty() ? "  (none due today)" : dueStr,
                       LocalDate.now(), dailyGoalMins, dailyGoalMins);

        try {
            String json = gemini.generateContent(
                "You are a study planning agent. Return only valid JSON.", prompt);
            // Clean up markdown fences if present
            json = json.strip();
            if (json.startsWith("```")) {
                json = json.replaceAll("```json|```", "").strip();
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> parsed = om.readValue(json, Map.class);
            return parsed;
        } catch (Exception e) {
            log.warn("[Planner] Gemini plan generation failed: {} — using fallback", e.getMessage());
            return buildFallbackPlan(dueToday, dailyGoalMins);
        }
    }

    private Map<String, Object> buildFallbackPlan(List<UserTopicProgress> dueToday, int dailyGoalMins) {
        List<Map<String, Object>> sessions = new ArrayList<>();
        int remaining = dailyGoalMins;
        for (UserTopicProgress p : dueToday.subList(0, Math.min(3, dueToday.size()))) {
            int mins = Math.min(25, remaining);
            sessions.add(Map.of(
                "type", "review",
                "topicId", p.getTopicId(),
                "topicName", humanTopicName(p.getTopicId()),
                "subjectId", p.getSubjectId() != null ? p.getSubjectId() : "general",
                "subjectName", capitalize(p.getSubjectId() != null ? p.getSubjectId() : "General"),
                "durationMins", mins,
                "reason", "Due for spaced repetition review"
            ));
            remaining -= mins;
            if (remaining <= 0) break;
        }
        if (remaining > 15) {
            sessions.add(Map.of(
                "type", "practice",
                "topicId", "general",
                "topicName", "General Practice",
                "subjectId", "general",
                "subjectName", "General",
                "durationMins", remaining,
                "reason", "Practice to reinforce your knowledge"
            ));
        }
        return Map.of(
            "planDate", LocalDate.now().toString(),
            "totalMinutes", dailyGoalMins,
            "sessions", sessions,
            "motivationMessage", "Every minute of study today brings you closer to your goal. Keep going!"
        );
    }

    private Optional<Map<String, Object>> loadCachedPlan(UUID userId, LocalDate date) {
        try {
            List<String> rows = jdbc.queryForList(
                "SELECT sessions_json::text, motivation_msg, total_minutes FROM daily_plans WHERE user_id = ? AND plan_date = ?",
                String.class, userId, date);
            // We need multiple columns — use a more specific query
            var results = jdbc.queryForList(
                "SELECT sessions_json::text as sj, motivation_msg as msg, total_minutes as mins FROM daily_plans WHERE user_id = ? AND plan_date = ?",
                userId, date);
            if (results.isEmpty()) return Optional.empty();
            Map<String, Object> row = results.get(0);
            String sessionsJson = (String) row.get("sj");
            String msg = (String) row.get("msg");
            int totalMins = ((Number) row.get("mins")).intValue();
            @SuppressWarnings("unchecked")
            List<Object> sessions = om.readValue(sessionsJson, List.class);
            Map<String, Object> plan = new LinkedHashMap<>();
            plan.put("planDate", date.toString());
            plan.put("totalMinutes", totalMins);
            plan.put("sessions", sessions);
            plan.put("motivationMessage", msg != null ? msg : "Study hard today!");
            return Optional.of(plan);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    private void savePlan(UUID userId, LocalDate date, Map<String, Object> plan) {
        try {
            String sessionsJson = om.writeValueAsString(plan.getOrDefault("sessions", List.of()));
            String msg = (String) plan.getOrDefault("motivationMessage", "");
            int totalMins = ((Number) plan.getOrDefault("totalMinutes", 120)).intValue();
            jdbc.update("""
                INSERT INTO daily_plans (user_id, plan_date, total_minutes, sessions_json, motivation_msg)
                VALUES (?, ?, ?, ?::jsonb, ?)
                ON CONFLICT (user_id, plan_date) DO UPDATE
                  SET sessions_json = EXCLUDED.sessions_json,
                      motivation_msg = EXCLUDED.motivation_msg
                """, userId, date, totalMins, sessionsJson, msg);
        } catch (Exception e) {
            log.warn("[Planner] Failed to save plan: {}", e.getMessage());
        }
    }

    private String humanTopicName(String topicId) {
        if (topicId == null) return "Topic";
        return Arrays.stream(topicId.split("-"))
            .skip(2) // skip "jee-physics-" prefix
            .map(this::capitalize)
            .collect(Collectors.joining(" "));
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    // ── Component D: Autonomous Agent Replan Loop ─────────────────────────

    /**
     * Called asynchronously from QuizService after every test submission.
     * The agent decides autonomously whether to rebuild today's plan based
     * on the student's current struggle indicator — no student input needed.
     *
     * Triggers:
     *   1. struggle_indicator > 0.6 (severe struggle detected)
     *   2. Too many practice sessions AND still struggling
     */
    @Async
    public void agentReplanIfNeeded(UUID userId) {
        try {
            LearnerProfile profile = profileRepo.findByUserId(userId).orElse(null);
            if (profile == null) return;

            double struggle = profile.getStruggleIndicator() != null
                ? profile.getStruggleIndicator().doubleValue() : 0;

            // Does a plan exist for today?
            boolean planExists = false;
            try {
                planExists = !jdbc.queryForList(
                    "SELECT 1 FROM daily_plans WHERE user_id = ? AND plan_date = CURRENT_DATE",
                    userId).isEmpty();
            } catch (Exception e) {
                return; // No plan table or no plan — nothing to replan
            }
            if (!planExists) return;

            boolean shouldReplan = false;
            String replanReason = "";

            if (struggle > 0.6) {
                shouldReplan = true;
                replanReason = "recent tests show severe struggle (>" +
                    String.format("%.0f", struggle * 100) + "% error rate)";
            }

            if (!shouldReplan) return;

            log.info("[PlannerAgent] Autonomous replan for userId={}: {}", userId, replanReason);

            // Delete existing plan (will be regenerated fresh on next GET /api/planner/today)
            jdbc.update("DELETE FROM daily_plans WHERE user_id = ? AND plan_date = CURRENT_DATE",
                userId);

            // Notify student
            notificationService.createNotification(userId, "ai_insight",
                "Your study plan was updated",
                "Based on your recent performance, PrepCreatine adjusted today's plan " +
                "to focus more on review. " + replanReason + ".",
                "/planner");

            log.info("[PlannerAgent] Plan invalidated for userId={} — will regenerate on next request", userId);

        } catch (Exception e) {
            log.warn("[PlannerAgent] Replan failed userId={}: {}", userId, e.getMessage());
        }
    }
}

