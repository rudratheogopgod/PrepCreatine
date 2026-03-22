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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

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

    // ── Python Planner Agent config ────────────────────────────────────────
    @Value("${planner.agent.url:http://localhost:8001}")
    private String plannerAgentUrl;

    @Value("${planner.agent.enabled:true}")
    private boolean plannerAgentEnabled;

    @Autowired
    private RestTemplate restTemplate;

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
     * Primary: fetches from Python LangGraph planner agent (agent_study_plan table).
     * Fallback: generates via Gemini if agent is not running or fails.
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

        // 3. Load review-due topics (always computed regardless of agent)
        List<UserTopicProgress> dueToday = spacedRep.getDueToday(userId);

        // 4. Try the Python LangGraph planner agent
        if (plannerAgentEnabled) {
            try {
                Map<String, Object> agentPlan = buildPlanViaAgent(userId, ctx, examId, dailyGoal, dueToday);
                if (agentPlan != null) {
                    savePlan(userId, LocalDate.now(), agentPlan);
                    return agentPlan;
                }
            } catch (Exception e) {
                log.warn("[Planner] Agent call failed ({}) — falling back to Gemini", e.getMessage());
            }
        }

        // 5. Fallback: generate via Gemini (existing path unchanged)
        String[] weakTopics = ctx != null && ctx.getWeakTopics() != null ? ctx.getWeakTopics() : new String[0];
        Map<String, Object> plan = buildPlan(userId, examId, dailyGoal, dueToday, weakTopics, ctx);
        savePlan(userId, LocalDate.now(), plan);
        return plan;
    }

    /**
     * Calls the Python LangGraph planner agent to get today's sessions.
     * Returns null if the agent has no plan yet AND plan generation fails.
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> buildPlanViaAgent(UUID userId, UserContext ctx,
                                                   String examId, int dailyGoal,
                                                   List<UserTopicProgress> dueToday) {
        String userIdStr = userId.toString();

        // Step 1 — check if agent plan exists
        String checkUrl = plannerAgentUrl + "/check-plan/" + userIdStr;
        Map<?, ?> checkResp = restTemplate.getForObject(checkUrl, Map.class);
        boolean planExists = checkResp != null && Boolean.TRUE.equals(checkResp.get("plan_exists"));

        // Step 2 — if not, generate one (~15-40s). Runs synchronously here.
        if (!planExists) {
            log.info("[Planner] No agent plan — generating for userId={}", userId);
            String genUrl = plannerAgentUrl + "/generate-plan";
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("user_id",          userIdStr);
            body.put("exam_name",         ctx != null && ctx.getExamType() != null
                ? ctx.getExamType().toUpperCase() : "JEE");
            body.put("target_exam_year",  ctx != null && ctx.getExamDate() != null
                ? ctx.getExamDate().getYear() : 2026);
            body.put("daily_study_hours", dailyGoal / 60.0);
            body.put("student_level",     "Intermediate");
            body.put("plan_start_date",   LocalDate.now().toString());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            ResponseEntity<Map> genResp = restTemplate.postForEntity(
                genUrl, new HttpEntity<>(body, headers), Map.class);
            if (!genResp.getStatusCode().is2xxSuccessful()) {
                log.warn("[Planner] Agent plan generation failed: {}", genResp.getStatusCode());
                return null;
            }
            log.info("[Planner] Agent plan generated: {} sessions",
                genResp.getBody() != null ? genResp.getBody().get("total_sessions") : "?");
        }

        // Step 3 — compute week + day position since plan start
        LocalDate startDate = ctx != null && ctx.getCreatedAt() != null
            ? ctx.getCreatedAt().toLocalDate() : LocalDate.now();
        long daysSince = ChronoUnit.DAYS.between(startDate, LocalDate.now());
        int currentWeek = (int)(daysSince / 7) + 1;
        int currentDay  = (int)(daysSince % 7) + 1;

        // Step 4 — fetch today's agent sessions
        String todayUrl = plannerAgentUrl + "/today-sessions/" + userIdStr
            + "?week=" + currentWeek + "&day=" + currentDay;
        Map<?, ?> todayResp = restTemplate.getForObject(todayUrl, Map.class);
        List<Map<String, Object>> agentSessions = todayResp != null
            ? (List<Map<String, Object>>) todayResp.get("sessions")
            : Collections.emptyList();

        if (agentSessions == null || agentSessions.isEmpty()) {
            log.warn("[Planner] Agent returned 0 sessions for userId={} week={} day={}",
                userId, currentWeek, currentDay);
            return null;
        }

        // Step 5 — prepend SM-2 review-due sessions (max 2)
        List<Map<String, Object>> sessions = new ArrayList<>();
        Set<String> reviewTopicKeys = new HashSet<>();

        dueToday.stream().limit(2).forEach(r -> {
            String topicName = humanTopicName(r.getTopicId());
            reviewTopicKeys.add((r.getSubjectId() != null ? r.getSubjectId() : "") + "::" + topicName);
            sessions.add(Map.of(
                "type",        "review",
                "topicId",     r.getTopicId(),
                "topicName",   topicName,
                "subjectId",   r.getSubjectId() != null ? r.getSubjectId() : "general",
                "subjectName", capitalize(r.getSubjectId() != null ? r.getSubjectId() : "General"),
                "durationMins", 20,
                "reason",      "Spaced repetition due today — forgetting curve requires review"
            ));
        });

        // Step 6 — merge agent sessions (skipping anything already in review)
        for (Map<String, Object> s : agentSessions) {
            String subj  = (String) s.getOrDefault("subject", "General");
            String topic = (String) s.getOrDefault("topic", "Topic");
            String key   = subj.toLowerCase() + "::" + topic;
            if (!reviewTopicKeys.contains(key)) {
                int durationMins = s.get("duration_mins") != null
                    ? ((Number) s.get("duration_mins")).intValue() : 60;
                Map<String, Object> session = new LinkedHashMap<>();
                session.put("type",        s.getOrDefault("session_type", "new"));
                session.put("topicId",     examId + "-" + subj.toLowerCase().replace(" ", "-")
                    + "-" + topic.toLowerCase().replace(" ", "-").substring(0, Math.min(20, topic.length())));
                session.put("topicName",   topic);
                session.put("subjectId",   subj.toLowerCase().replace(" ", "-"));
                session.put("subjectName", subj);
                session.put("durationMins", durationMins);
                session.put("reason",      "From your " + examId.toUpperCase() + " study plan (LangGraph agent)");
                sessions.add(session);
            }
        }

        int totalMins = sessions.stream()
            .mapToInt(s -> ((Number) s.getOrDefault("durationMins", 0)).intValue()).sum();

        long daysToExam = ctx != null && ctx.getExamDate() != null
            ? ChronoUnit.DAYS.between(LocalDate.now(), ctx.getExamDate()) : -1;
        String motivationMsg = dueToday.size() > 0
            ? String.format("You have %d spaced repetition reviews due today. %s days to %s.",
                Math.min(2, dueToday.size()), daysToExam > 0 ? daysToExam : "?", examId.toUpperCase())
            : String.format("Today's %d-minute plan keeps you on track. %s days to %s — every session counts.",
                totalMins, daysToExam > 0 ? daysToExam : "?", examId.toUpperCase());

        Map<String, Object> plan = new LinkedHashMap<>();
        plan.put("planDate",         LocalDate.now().toString());
        plan.put("totalMinutes",      totalMins);
        plan.put("sessions",          sessions);
        plan.put("motivationMessage", motivationMsg);
        plan.put("source",            "langgraph-agent");

        log.info("[Planner] Agent plan for userId={}: {} sessions ({} SM-2 reviews + {} agent sessions)",
            userId, sessions.size(), Math.min(2, dueToday.size()), agentSessions.size());

        return plan;
    }

    /**
     * Notifies the Python agent of a session's completion so it can
     * autonomously rebalance tomorrow's plan if needed.
     * Call from markSessionComplete() after updating the daily_plans table.
     */
    @Async
    @SuppressWarnings("unchecked")
    public void notifyAgentOfProgress(UUID userId, String subject, String topicName,
                                      double completionPct) {
        if (!plannerAgentEnabled) return;
        try {
            UserContext ctx = contextRepo.findByUserId(userId).orElse(null);
            LocalDate startDate = ctx != null && ctx.getCreatedAt() != null
                ? ctx.getCreatedAt().toLocalDate() : LocalDate.now();
            long daysSince = ChronoUnit.DAYS.between(startDate, LocalDate.now());
            int week = (int)(daysSince / 7) + 1;
            int day  = (int)(daysSince % 7) + 1;

            Map<String, Object> body = new LinkedHashMap<>();
            body.put("user_id",      userId.toString());
            body.put("current_week", week);
            body.put("current_day",  day);
            Map<String, Double> progressMap = new LinkedHashMap<>();
            progressMap.put(subject + "::" + topicName, completionPct);
            body.put("progress_map", progressMap);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            ResponseEntity<Map> resp = restTemplate.postForEntity(
                plannerAgentUrl + "/track-progress",
                new HttpEntity<>(body, headers), Map.class);

            if (resp.getBody() != null && Boolean.TRUE.equals(resp.getBody().get("was_rebalanced"))) {
                List<Map<?,?>> catchUp = (List<Map<?,?>>) resp.getBody().get("catch_up_sessions_added");
                int added = catchUp != null ? catchUp.size() : 0;
                log.info("[Planner] AUTONOMOUS REBALANCING: {} catch-up sessions added for userId={}",
                    added, userId);

                // Invalidate tomorrow's cached plan so it picks up the new sessions
                LocalDate tomorrow = LocalDate.now().plusDays(1);
                jdbc.update("DELETE FROM daily_plans WHERE user_id = ? AND plan_date = ?",
                    userId, tomorrow);

                // Notify student
                notificationService.createNotification(userId, "ai_insight",
                    "Study plan updated automatically",
                    added + " catch-up session" + (added == 1 ? "" : "s") +
                    " added to tomorrow's plan based on today's progress.",
                    "/planner");
            }
        } catch (Exception e) {
            log.warn("[Planner] Agent progress notify failed (non-fatal): {}", e.getMessage());
        }
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

