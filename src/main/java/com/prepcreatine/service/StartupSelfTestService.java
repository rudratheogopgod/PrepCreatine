package com.prepcreatine.service;

import com.prepcreatine.config.DemoModeConfig;
import com.prepcreatine.repository.*;
import com.prepcreatine.util.EvalLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

/**
 * StartupSelfTestService — 10 automated health checks at startup.
 *
 * Verifies all demo data is seeded and every module is ready before judges
 * interact with the system. Results are written to both console and
 * logs/hackathon-eval.log for submission screenshots.
 *
 * Section 13 of the logging specification.
 */
@Service
public class StartupSelfTestService {

    private static final Logger log = LoggerFactory.getLogger(StartupSelfTestService.class);

    private final UserRepository              userRepository;
    private final UserTopicProgressRepository userTopicProgressRepository;
    private final TestSessionRepository       testSessionRepository;
    private final SystemSourceRepository      systemSourceRepository;
    private final QuestionRepository          questionRepository;
    private final LearnerProfileRepository    learnerProfileRepository;
    private final JdbcTemplate                jdbc;

    public StartupSelfTestService(UserRepository userRepository,
                                  UserTopicProgressRepository userTopicProgressRepository,
                                  TestSessionRepository testSessionRepository,
                                  SystemSourceRepository systemSourceRepository,
                                  QuestionRepository questionRepository,
                                  LearnerProfileRepository learnerProfileRepository,
                                  JdbcTemplate jdbc) {
        this.userRepository              = userRepository;
        this.userTopicProgressRepository = userTopicProgressRepository;
        this.testSessionRepository       = testSessionRepository;
        this.systemSourceRepository      = systemSourceRepository;
        this.questionRepository          = questionRepository;
        this.learnerProfileRepository    = learnerProfileRepository;
        this.jdbc                        = jdbc;
    }

    /** Runs last after all beans and seeders have had time to complete. */
    @EventListener(ApplicationReadyEvent.class)
    @Order(Ordered.LOWEST_PRECEDENCE)
    public void runSelfTest() {
        try { Thread.sleep(3000); } catch (InterruptedException ignored) {}

        EvalLogger.separator(log);
        log.info("[SelfTest] ══════════════════════════════════════════");
        log.info("[SelfTest]   PREPCREATINE STARTUP SELF-TEST RESULTS  ");
        log.info("[SelfTest]   DevClash2026 | NIT Raipur | Hackathon   ");
        log.info("[SelfTest] ══════════════════════════════════════════");

        int passed = 0, total = 0;

        // TEST 1: Database connectivity
        total++;
        try {
            long userCount = userRepository.count();
            EvalLogger.success(log, "SELF-TEST", "DB connected: " + userCount + " users in DB ✓");
            passed++;
        } catch (Exception e) {
            EvalLogger.failure(log, "SELF-TEST", "DB connection FAILED: " + e.getMessage());
        }

        // TEST 2: Demo user 'Arjun Sharma' exists
        total++;
        try {
            boolean exists = userRepository.existsById(DemoModeConfig.DEMO_USER_ID);
            if (exists) {
                EvalLogger.success(log, "SELF-TEST", "Demo user 'Arjun Sharma' exists in DB ✓");
                passed++;
            } else {
                EvalLogger.failure(log, "SELF-TEST", "Demo user NOT found — seeder may have failed");
            }
        } catch (Exception e) {
            EvalLogger.failure(log, "SELF-TEST", "Demo user check failed: " + e.getMessage());
        }

        // TEST 3: SM-2 topics due today
        total++;
        try {
            long dueCount = userTopicProgressRepository
                .countDueForReview(DemoModeConfig.DEMO_USER_ID, LocalDate.now());
            if (dueCount >= 5) {
                EvalLogger.success(log, "SELF-TEST",
                    "SM-2 review queue: " + dueCount + " topics due today ✓ (Module 1 ready)");
                passed++;
            } else {
                EvalLogger.failure(log, "SELF-TEST",
                    "SM-2 review queue has only " + dueCount + " topics — expected 5+");
            }
        } catch (Exception e) {
            EvalLogger.failure(log, "SELF-TEST", "SM-2 check failed: " + e.getMessage());
        }

        // TEST 4: Daily plan pre-seeded (checked via JDBC — no JPA entity for daily_plans)
        total++;
        try {
            Integer planCount = jdbc.queryForObject(
                "SELECT COUNT(*) FROM daily_plans WHERE user_id = ?::uuid AND plan_date = CURRENT_DATE",
                Integer.class, DemoModeConfig.DEMO_USER_ID.toString());
            if (planCount != null && planCount > 0) {
                EvalLogger.success(log, "SELF-TEST",
                    "Daily plan pre-seeded for today ✓ (Module 2 ready — no Gemini call on first load)");
                passed++;
            } else {
                EvalLogger.failure(log, "SELF-TEST",
                    "Daily plan NOT seeded for today — first load will call Gemini");
            }
        } catch (Exception e) {
            EvalLogger.failure(log, "SELF-TEST", "Daily plan check failed: " + e.getMessage());
        }

        // TEST 5: Targeted drill sessions exist
        total++;
        try {
            long drillCount = testSessionRepository
                .countByUserIdAndStatus(DemoModeConfig.DEMO_USER_ID, "in_progress");
            if (drillCount >= 1) {
                EvalLogger.success(log, "SELF-TEST",
                    "Targeted drills ready: " + drillCount + " in-progress sessions ✓ (Module 3 ready)");
                passed++;
            } else {
                EvalLogger.failure(log, "SELF-TEST",
                    "No in-progress drill sessions found — expected 2+");
            }
        } catch (Exception e) {
            EvalLogger.failure(log, "SELF-TEST", "Drill check failed: " + e.getMessage());
        }

        // TEST 6: NCERT system corpus seeded
        total++;
        try {
            long corpusCount = systemSourceRepository.count();
            if (corpusCount >= 5) {
                EvalLogger.success(log, "SELF-TEST",
                    "NCERT corpus: " + corpusCount + " sources seeded ✓ (Module 4 RAG ready)");
                passed++;
            } else {
                EvalLogger.failure(log, "SELF-TEST",
                    "NCERT corpus has only " + corpusCount + " sources — expected 5+");
            }
        } catch (Exception e) {
            EvalLogger.failure(log, "SELF-TEST", "Corpus check failed: " + e.getMessage());
        }

        // TEST 7: Question bank has demo questions
        total++;
        try {
            long questionCount = questionRepository.count();
            if (questionCount >= 6) {
                EvalLogger.success(log, "SELF-TEST",
                    "Question bank: " + questionCount + " questions ✓ (tests can start without AI gen)");
                passed++;
            } else {
                EvalLogger.failure(log, "SELF-TEST",
                    "Question bank has only " + questionCount + " questions — expected 6+");
            }
        } catch (Exception e) {
            EvalLogger.failure(log, "SELF-TEST", "Question bank check failed: " + e.getMessage());
        }

        // TEST 8: Concept graph nodes seeded (JDBC — no JPA repo for concept_graph_nodes)
        total++;
        try {
            Integer nodeCount = jdbc.queryForObject(
                "SELECT COUNT(*) FROM concept_graph_nodes WHERE user_id = ?::uuid",
                Integer.class, DemoModeConfig.DEMO_USER_ID.toString());
            if (nodeCount != null && nodeCount >= 10) {
                EvalLogger.success(log, "SELF-TEST",
                    "Knowledge graph: " + nodeCount + " concept nodes ✓");
                passed++;
            } else {
                EvalLogger.failure(log, "SELF-TEST",
                    "Only " + nodeCount + " concept nodes — expected 10+");
            }
        } catch (Exception e) {
            EvalLogger.failure(log, "SELF-TEST", "Concept graph check failed: " + e.getMessage());
        }

        // TEST 9: Gemini API key is configured
        total++;
        String geminiKey = System.getenv("GEMINI_API_KEY");
        boolean geminiKeyPresent = geminiKey != null && !geminiKey.isBlank();
        if (geminiKeyPresent) {
            EvalLogger.success(log, "SELF-TEST", "Gemini API key configured ✓");
            passed++;
        } else {
            EvalLogger.failure(log, "SELF-TEST",
                "GEMINI_API_KEY is NOT set — AI features will fail");
        }

        // TEST 10: Learner profile seeded (AI agent behavioral data)
        total++;
        try {
            boolean profileExists = learnerProfileRepository
                .findByUserId(DemoModeConfig.DEMO_USER_ID).isPresent();
            if (profileExists) {
                EvalLogger.success(log, "SELF-TEST",
                    "Learner profile seeded — AI agent has behavioral data ✓");
                passed++;
            } else {
                EvalLogger.failure(log, "SELF-TEST",
                    "Learner profile NOT seeded — /api/analytics/agent-insights will be empty");
            }
        } catch (Exception e) {
            EvalLogger.failure(log, "SELF-TEST", "Learner profile check failed: " + e.getMessage());
        }

        // FINAL RESULT
        log.info("[SelfTest] ══════════════════════════════════════════");
        if (passed == total) {
            EvalLogger.success(log, "SELF-TEST",
                String.format("ALL %d/%d TESTS PASSED — SYSTEM IS READY FOR DEMO ✓", passed, total));
        } else {
            EvalLogger.failure(log, "SELF-TEST",
                String.format("%d/%d tests passed — %d failure(s), check logs above",
                    passed, total, total - passed));
        }
        log.info("[SelfTest] ══════════════════════════════════════════");
        EvalLogger.separator(log);
    }
}
