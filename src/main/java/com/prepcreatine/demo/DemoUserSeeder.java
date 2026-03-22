package com.prepcreatine.demo;

import com.prepcreatine.config.DemoModeConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Seeds all demo data on application startup.
 * BSDD v2.1 §5A: DemoUserSeeder.java [NEW]
 *
 * All seed methods are idempotent — INSERT ... ON CONFLICT DO NOTHING.
 * Only active when app.demo-mode=true.
 */
@Component
@ConditionalOnProperty(name = "app.demo-mode", havingValue = "true")
public class DemoUserSeeder {

    private static final Logger log = LoggerFactory.getLogger(DemoUserSeeder.class);
    private static final String USER_ID = DemoModeConfig.DEMO_USER_ID.toString();
    private static final String MENTOR_ID = "00000000-0000-0000-0000-000000000099";

    private final JdbcTemplate jdbc;

    public DemoUserSeeder(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Async
    public void seedDemoData() {
        log.info("[DemoUserSeeder] Starting demo data seed...");
        // Core user data — if this fails, throw to signal fatal error
        seedUser();
        seedUserContext();

        // Non-critical seeding — failures logged and skipped
        tryRun("syllabusProgress", this::seedSyllabusProgress);
        tryRun("sessions",         this::seedSessions);
        tryRun("conversations",    this::seedConversations);
        tryRun("sources",          this::seedSources);
        tryRun("testSessions",     this::seedTestSessions);
        tryRun("testAnswers",      this::seedTestAnswers);
        tryRun("conceptGraph",     this::seedConceptGraphNodes);
        tryRun("community",        this::seedCommunityData);
        tryRun("mentor",           this::seedMentorData);
        tryRun("dailyPlan",        this::seedDailyPlan);
        tryRun("notifications",    this::seedNotifications);
        tryRun("questions",        this::seedQuestions);
        tryRun("learnerProfile",   this::seedLearnerProfile);
        tryRun("memoryEntries",    this::seedMemoryEntries);

        log.info("[DemoUserSeeder] Demo data seed complete.");
    }

    private void tryRun(String method, Runnable r) {
        try {
            log.info("[DemoUserSeeder] Seeding {}: start", method);
            r.run();
            log.info("[DemoUserSeeder] Seeding {}: complete", method);
        } catch (Exception e) {
            log.warn("[DemoUserSeeder] Seeding {}: failed - {}", method, e.getMessage());
        }
    }

    // ── Core (fatal if fails) ────────────────────────────────────────────────

    @Transactional
    protected void seedUser() {
        log.info("[DemoUserSeeder] Seeding user: start");
        jdbc.update("""
            INSERT INTO users (id, email, password_hash, full_name, role,
              is_email_verified, onboarding_complete, share_token,
              exam_type, exam_date, study_mode, daily_goal_mins,
              current_streak, longest_streak, total_days, readiness_score)
            VALUES (?::uuid, ?, ?, ?, ?, true, true, ?,
                    'jee', CURRENT_DATE + INTERVAL '60 days', 'in_depth', 120,
                    14, 14, 20, 61)
            ON CONFLICT (id) DO UPDATE SET
              current_streak = 14,
              readiness_score = 61,
              exam_date = CURRENT_DATE + INTERVAL '60 days'
            """,
            USER_ID,
            "arjun@prepcreatine.demo",
            "$2a$12$demoHashNotRealDoNotUseThisInProduction1234567890abc",
            "Arjun Sharma",
            "STUDENT",
            "demo-share-token-arjun-2026"
        );
        log.info("[DemoUserSeeder] Seeding user: complete");
    }

    @Transactional
    protected void seedUserContext() {
        log.info("[DemoUserSeeder] Seeding userContext: start");
        jdbc.update("""
            INSERT INTO user_contexts (id, user_id, exam_type, exam_date,
              study_mode, daily_goal_mins, weak_topics, strong_topics, theme)
            VALUES (?::uuid, ?::uuid, 'jee',
                    CURRENT_DATE + INTERVAL '60 days',
                    'in_depth', 120,
                    ARRAY['jee-physics-thermodynamics','jee-chemistry-organic-goc',
                          'jee-math-integration','jee-physics-optics'],
                    ARRAY['jee-physics-kinematics','jee-math-coordinates'],
                    'system')
            ON CONFLICT (user_id) DO UPDATE SET
              exam_date = CURRENT_DATE + INTERVAL '60 days'
            """,
            "00000000-0000-0000-0000-000000000010",
            USER_ID
        );
        log.info("[DemoUserSeeder] Seeding userContext: complete");
    }

    // ── Syllabus Progress ────────────────────────────────────────────────────

    public void seedSyllabusProgress() {
        // 45 done topics
        upsertProgress("jee-physics-kinematics",         "done", 2.8, 5, 24,  24);
        upsertProgress("jee-physics-laws-of-motion",     "done", 2.6, 4, 14,   7);
        upsertProgress("jee-physics-work-energy",        "done", 2.5, 3,  8,   0);
        upsertProgress("jee-physics-circular-motion",    "done", 2.4, 2,  6,   0);
        upsertProgress("jee-physics-gravitation",        "done", 2.3, 1,  1,  -1);
        upsertProgress("jee-physics-electrostatics",     "done", 2.5, 3, 10,   3);
        upsertProgress("jee-physics-current-electricity","done", 2.2, 2,  6,   0);
        upsertProgress("jee-physics-magnetic-effects",   "done", 2.4, 2,  6,   5);
        upsertProgress("jee-physics-emi",                "done", 2.1, 1,  1,  -1);
        upsertProgress("jee-physics-waves",              "done", 2.6, 4, 15,  10);
        upsertProgress("jee-physics-units-dimensions",   "done", 2.8, 5, 22,  18);
        upsertProgress("jee-physics-rotational-motion",  "done", 2.3, 2,  6,   2);
        upsertProgress("jee-physics-fluid-mechanics",    "done", 2.2, 1,  1,  -1);
        upsertProgress("jee-physics-simple-harmonic-motion","done",2.5,3, 9,   6);
        upsertProgress("jee-physics-nuclear-physics",    "done", 2.0, 1,  1,  -1);

        upsertProgress("jee-chemistry-mole-concept",    "done", 2.5, 5, 0, 20);
        upsertProgress("jee-chemistry-atomic-structure","done", 2.5, 4, 0, 12);
        upsertProgress("jee-chemistry-periodic-table",  "done", 2.5, 4, 0, 11);
        upsertProgress("jee-chemistry-chemical-bonding","done", 2.5, 3, 0,  0);
        upsertProgress("jee-chemistry-states-of-matter","done", 2.5, 3, 0,  4);
        upsertProgress("jee-chemistry-thermodynamics",  "done", 2.5, 2, 0,  2);
        upsertProgress("jee-chemistry-equilibrium",     "done", 2.5, 2, 0,  0);
        upsertProgress("jee-chemistry-redox-reactions", "done", 2.5, 3, 0,  7);
        upsertProgress("jee-chemistry-solutions",       "done", 2.5, 2, 0,  3);
        upsertProgress("jee-chemistry-electrochemistry","done", 2.5, 1, 0, -1);
        upsertProgress("jee-chemistry-solid-state",     "done", 2.5, 2, 0,  1);
        upsertProgress("jee-chemistry-surface-chemistry","done",2.5, 2, 0,  5);
        upsertProgress("jee-chemistry-coordination",    "done", 2.5, 1, 0, -1);
        upsertProgress("jee-chemistry-halogen-derivatives","done",2.5,1, 0,  0);
        upsertProgress("jee-chemistry-alcohol-phenol",  "done", 2.5, 1, 0,  1);

        upsertProgress("jee-math-sets-functions",      "done", 2.5, 5, 0, 25);
        upsertProgress("jee-math-relations",           "done", 2.5, 4, 0, 13);
        upsertProgress("jee-math-trigonometry",        "done", 2.5, 4, 0, 10);
        upsertProgress("jee-math-complex-numbers",     "done", 2.5, 3, 0,  8);
        upsertProgress("jee-math-quadratic-equations", "done", 2.5, 3, 0,  5);
        upsertProgress("jee-math-sequences-series",    "done", 2.5, 3, 0,  0);
        upsertProgress("jee-math-permutations",        "done", 2.5, 2, 0,  3);
        upsertProgress("jee-math-binomial-theorem",    "done", 2.5, 2, 0,  0);
        upsertProgress("jee-math-coordinates",         "done", 2.8, 5, 0, 28);
        upsertProgress("jee-math-straight-lines",      "done", 2.5, 4, 0, 14);
        upsertProgress("jee-math-circles",             "done", 2.5, 3, 0,  9);
        upsertProgress("jee-math-parabola",            "done", 2.5, 2, 0, -1);
        upsertProgress("jee-math-limits-continuity",   "done", 2.5, 3, 0,  6);
        upsertProgress("jee-math-differentiation",     "done", 2.5, 2, 0,  4);
        upsertProgress("jee-math-applications-derivatives","done",2.5,2,0, 2);

        // 10 in_progress topics
        upsertProgressInProgress("jee-physics-thermodynamics",       85);
        upsertProgressInProgress("jee-physics-optics",               45);
        upsertProgressInProgress("jee-chemistry-organic-goc",       120);
        upsertProgressInProgress("jee-chemistry-aldehydes-ketones",  30);
        upsertProgressInProgress("jee-chemistry-carboxylic-acids",   20);
        upsertProgressInProgress("jee-math-integration",             95);
        upsertProgressInProgress("jee-math-differential-equations",  40);
        upsertProgressInProgress("jee-math-vectors",                 55);
        upsertProgressInProgress("jee-math-3d-geometry",             25);
        upsertProgressInProgress("jee-math-probability",             35);

        // 5 not_started topics
        upsertProgressNotStarted("jee-physics-semiconductors");
        upsertProgressNotStarted("jee-physics-communication");
        upsertProgressNotStarted("jee-chemistry-polymers");
        upsertProgressNotStarted("jee-chemistry-biomolecules");
        upsertProgressNotStarted("jee-math-statistics");
    }

    private void upsertProgress(String topicId, String status,
                                double easiness, int rep, int interval, int reviewOffsetDays) {
        jdbc.update("""
            INSERT INTO user_topic_progress
              (user_id, topic_id, status, easiness_factor, repetition_count,
               interval_days, next_review_date, last_reviewed_at)
            VALUES (?::uuid, ?, ?, ?, ?, ?, CURRENT_DATE + ? * INTERVAL '1 day', NOW())
            ON CONFLICT (user_id, topic_id) DO NOTHING
            """,
            USER_ID, topicId, status, easiness, rep, interval, reviewOffsetDays);
    }

    private void upsertProgressInProgress(String topicId, int timeSpent) {
        jdbc.update("""
            INSERT INTO user_topic_progress
              (user_id, topic_id, status, time_spent_mins)
            VALUES (?::uuid, ?, 'in_progress', ?)
            ON CONFLICT (user_id, topic_id) DO NOTHING
            """,
            USER_ID, topicId, timeSpent);
    }

    private void upsertProgressNotStarted(String topicId) {
        jdbc.update("""
            INSERT INTO user_topic_progress (user_id, topic_id, status)
            VALUES (?::uuid, ?, 'not_started')
            ON CONFLICT (user_id, topic_id) DO NOTHING
            """,
            USER_ID, topicId);
    }

    // ── Sessions (study streak) ──────────────────────────────────────────────

    private void seedSessions() {
        Object[][] sessions = {
            {0,  150, "in_depth",  "jee-physics-kinematics,jee-physics-laws-of-motion"},
            {1,  120, "revision",  "jee-physics-work-energy,jee-physics-gravitation"},
            {2,   90, "in_depth",  "jee-chemistry-chemical-bonding,jee-chemistry-equilibrium"},
            {3,  180, "in_depth",  "jee-chemistry-mole-concept,jee-chemistry-atomic-structure"},
            {4,   75, "speed_run", "jee-math-sets-functions,jee-math-trigonometry"},
            {5,  130, "in_depth",  "jee-math-complex-numbers,jee-math-sequences-series"},
            {6,   60, "revision",  "jee-physics-circular-motion,jee-physics-fluid-mechanics"},
            {7,  110, "in_depth",  "jee-physics-electrostatics,jee-physics-current-electricity"},
            {8,   95, "revision",  "jee-chemistry-solid-state,jee-chemistry-solutions"},
            {9,  145, "in_depth",  "jee-math-coordinates,jee-math-straight-lines"},
            {10,  80, "speed_run", "jee-math-limits-continuity,jee-math-differentiation"},
            {11, 160, "in_depth",  "jee-chemistry-redox-reactions,jee-chemistry-electrochemistry"},
            {12,  70, "revision",  "jee-math-parabola,jee-math-circles"},
            {13, 100, "in_depth",  "jee-physics-rotational-motion,jee-physics-simple-harmonic-motion"},
            {14,  55, "revision",  "jee-chemistry-periodic-table,jee-chemistry-chemical-bonding"},
            {16,  45, "in_depth",  "jee-physics-units-dimensions"},
            {18,  90, "in_depth",  "jee-physics-waves,jee-physics-nuclear-physics"},
            {20,  60, "speed_run", "jee-math-quadratic-equations"},
        };
        for (Object[] s : sessions) {
            int dayOffset = (int) s[0];
            int duration  = (int) s[1];
            String mode   = (String) s[2];
            String topics = (String) s[3];
            jdbc.update("""
                INSERT INTO sessions (user_id, date, duration_mins, mode_used, topics_touched)
                VALUES (?::uuid, CURRENT_DATE - ? * INTERVAL '1 day', ?, ?, ARRAY[?]::text[])
                ON CONFLICT (user_id, date) DO NOTHING
                """,
                USER_ID, dayOffset, duration, mode, topics);
        }
    }

    // ── Conversations ────────────────────────────────────────────────────────

    private void seedConversations() {
        seedConversation("00000000-0000-0000-0000-000000000002",
            "Thermodynamics doubt",
            "I don't understand the second law of thermodynamics. How does entropy always increase?",
            "Great question! The second law tells us that in any spontaneous process, the total entropy of the universe always increases or stays the same — it never decreases spontaneously. Think of it this way: if you drop an ice cube in warm water, heat flows from warm to cold. This is spontaneous. The reverse never happens because it would decrease entropy.");

        seedConversation("00000000-0000-0000-0000-000000000003",
            "Integration by parts",
            "Explain integration by parts with a JEE example",
            "Integration by parts uses the formula ∫u dv = uv - ∫v du. Use the LIATE rule (Logarithmic, Inverse trig, Algebraic, Trig, Exponential) to choose u. Example: ∫x·sin(x)dx — let u=x, dv=sin(x)dx → du=dx, v=-cos(x). Result: -x·cos(x) + ∫cos(x)dx = -x·cos(x) + sin(x) + C.");

        seedConversation("00000000-0000-0000-0000-000000000004",
            "Organic chemistry GOC",
            "What are inductive and mesomeric effects?",
            "The inductive effect (+I/-I) operates through sigma bonds and decreases with distance. -I groups withdraw electrons: -NO2 > -CN > -COOH > -F. The mesomeric effect (+M/-M) operates through pi bonds via resonance. +M groups donate electrons: -OH, -NH2, -OR. -M groups withdraw electrons: -NO2, -CHO, -COOH.");
    }

    private void seedConversation(String id, String title, String userMsg, String assistantMsg) {
        jdbc.update("""
            INSERT INTO conversations (id, user_id, title, exam_id)
            VALUES (?::uuid, ?::uuid, ?, 'jee')
            ON CONFLICT (id) DO NOTHING
            """, id, USER_ID, title);

        jdbc.update("""
            INSERT INTO messages (conversation_id, role, content)
            VALUES (?::uuid, 'user', ?)
            ON CONFLICT DO NOTHING
            """, id, userMsg);

        jdbc.update("""
            INSERT INTO messages (conversation_id, role, content)
            VALUES (?::uuid, 'assistant', ?)
            ON CONFLICT DO NOTHING
            """, id, assistantMsg);
    }

    // ── Sources ──────────────────────────────────────────────────────────────

    private void seedSources() {
        String thermoNotes = "Thermodynamics — Personal Notes\n" +
            "First Law: Energy cannot be created or destroyed. ΔU = Q - W.\n" +
            "Second Law: Entropy of universe always increases. dS = dQ_rev/T.\n" +
            "Third Law: Entropy of perfect crystal at 0K is zero.\n" +
            "Gibbs free energy: G = H - TS. Spontaneous if ΔG < 0.\n" +
            "Hess's Law: Enthalpy change same regardless of path.\n" +
            "Kirchhoff's equation: Cp - Cv = R for ideal gases.";

        String studyGuide1 = "{\"summary\":\"Covers laws of thermodynamics, state functions, entropy, and Gibbs free energy.\","
            + "\"topics\":[\"First Law\",\"Second Law\",\"Entropy\",\"Gibbs Energy\"],"
            + "\"formulas\":[\"ΔU = Q - W\",\"ΔS = dQ_rev/T\",\"G = H - TS\",\"Cp - Cv = R\"],"
            + "\"examQuestions\":[\"For isothermal reversible expansion what is ΔU?\","
            + "\"State the second law.\",\"What is the sign of ΔG for spontaneous process?\","
            + "\"Calculate entropy change if 1000J absorbed at 300K.\",\"Explain Hess's law.\"]}";

        jdbc.update("""
            INSERT INTO sources (id, user_id, type, status, raw_text, study_guide)
            VALUES (?::uuid, ?::uuid, 'text', 'ready', ?, ?::jsonb)
            ON CONFLICT (id) DO NOTHING
            """,
            "00000000-0000-0000-0000-000000000020", USER_ID, thermoNotes, studyGuide1);

        // Seed zero-vector source_chunks for source 1 (3 chunks)
        seedSourceChunk("00000000-0000-0000-0000-000000000020",
            "Thermodynamics First Law ... ΔU = Q - W ... internal energy state function.");
        seedSourceChunk("00000000-0000-0000-0000-000000000020",
            "Second Law ... entropy ... dS = dQ_rev/T ... irreversible ΔS_universe > 0.");
        seedSourceChunk("00000000-0000-0000-0000-000000000020",
            "Gibbs free energy ... G = H - TS ... spontaneous if ΔG < 0 ...");

        String integrationText = "Integration — Standard Methods\n" +
            "Integration by substitution: let u=g(x) if I = ∫f(g(x))g'(x)dx.\n" +
            "Integration by parts: ∫u dv = uv - ∫v du. Use LIATE rule.\n" +
            "Standard forms: ∫sin(x)dx = -cos(x)+C, ∫x^n dx = x^(n+1)/(n+1) + C.\n" +
            "Definite integrals: ∫_a^b f(x)dx = F(b) - F(a).";

        String studyGuide2 = "{\"summary\":\"Covers integration methods including substitution, by parts, and standard forms.\","
            + "\"topics\":[\"Substitution\",\"Integration by Parts\",\"Definite Integrals\"],"
            + "\"formulas\":[\"∫u dv = uv - ∫v du\",\"∫x^n dx = x^(n+1)/(n+1)+C\"],"
            + "\"examQuestions\":[\"Find ∫x·sin(x)dx.\",\"Apply LIATE to ∫x·e^x dx.\","
            + "\"Evaluate ∫_0^1 x^2 dx.\",\"Find ∫ln(x)dx.\",\"Solve ∫sin^2(x)dx.\"]}";

        jdbc.update("""
            INSERT INTO sources (id, user_id, type, status, raw_text, original_url, study_guide)
            VALUES (?::uuid, ?::uuid, 'url', 'ready', ?, ?, ?::jsonb)
            ON CONFLICT (id) DO NOTHING
            """,
            "00000000-0000-0000-0000-000000000021", USER_ID,
            integrationText, "https://ncert.nic.in/textbook.php?lemh1=7-13", studyGuide2);

        seedSourceChunk("00000000-0000-0000-0000-000000000021",
            "Integration by substitution: let u=g(x)... standard forms.");
        seedSourceChunk("00000000-0000-0000-0000-000000000021",
            "Definite integrals: F(b) - F(a). Area = ∫|f(x)|dx.");
    }

    private void seedSourceChunk(String sourceId, String content) {
        // Zero-vector placeholder per BSDD v2.1: real embeddings require API calls
        jdbc.update("""
            INSERT INTO source_chunks (source_id, content, embedding)
            VALUES (?::uuid, ?, array_fill(0, ARRAY[768])::vector)
            ON CONFLICT DO NOTHING
            """, sourceId, content);
    }

    // ── Test Sessions ────────────────────────────────────────────────────────

    private void seedTestSessions() {
        seedTestSession("00000000-0000-0000-0000-000000000030",
            "full_mock",      "jee",        null,                       null, 2, 90,  62.22, 56, "submitted", 7);
        seedTestSession("00000000-0000-0000-0000-000000000031",
            "topic_wise",     "jee-physics",null,                       null, 2, 20,  75.0,  15, "submitted", 5);
        seedTestSession("00000000-0000-0000-0000-000000000032",
            "topic_wise",     "jee-chemistry","jee-chemistry-organic-goc",null,2,20,  45.0,   9, "submitted", 4);
        seedTestSession("00000000-0000-0000-0000-000000000033",
            "rapid_fire",     "jee-math",   null,                       null, 1, 10,  80.0,   8, "submitted", 3);
        seedTestSession("00000000-0000-0000-0000-000000000034",
            "topic_wise",     "jee-math",   "jee-math-integration",     null, 2, 20,  40.0,   8, "submitted", 2);
        seedTestSession("00000000-0000-0000-0000-000000000035",
            "full_mock",      "jee",        null,                       null, 2, 90,  68.89, 62, "submitted", 1);
        seedTestSession("00000000-0000-0000-0000-000000000036",
            "targeted_practice","jee-chemistry","jee-chemistry-organic-goc",null,2, 3, null, null,"in_progress",0);
        seedTestSession("00000000-0000-0000-0000-000000000037",
            "targeted_practice","jee-math", "jee-math-integration",     null, 2,  3, null, null,"in_progress",0);
    }

    private void seedTestSession(String id, String type, String subjectId, String topicId,
                                 String examId, int level, int qCount,
                                 Double score, Integer correct, String status, int daysAgo) {
        jdbc.update("""
            INSERT INTO test_sessions
              (id, user_id, test_type, subject_id, topic_id, exam_id, level,
               question_count, score, correct_count, status, submitted_at)
            VALUES (?::uuid, ?::uuid, ?, ?, ?, ?, ?, ?, ?, ?, ?,
                    CASE WHEN ? > 0 THEN NOW() - ? * INTERVAL '1 day' ELSE NULL END)
            ON CONFLICT (id) DO NOTHING
            """,
            id, USER_ID, type, subjectId, topicId, examId, level,
            qCount, score, correct, status, daysAgo, daysAgo);
    }

    // ── Test Answers ─────────────────────────────────────────────────────────

    private void seedTestAnswers() {
        // Session 3 (Organic GOC — 45% score): 9 correct, 11 wrong
        String session3 = "00000000-0000-0000-0000-000000000032";
        seedAnswer(session3, "q1", "B", "A", false, 185);
        seedAnswer(session3, "q2", "A", "A", true,   42);
        seedAnswer(session3, "q3", "D", "C", false, 210);
        seedAnswer(session3, "q4", "B", "B", true,   38);
        seedAnswer(session3, "q5", "A", "C", false, 195);
        seedAnswer(session3, "q6", "C", "C", true,   55);
        seedAnswer(session3, "q7", "D", "B", false, 220);
        seedAnswer(session3, "q8", "A", "A", true,   40);
        seedAnswer(session3, "q9", "C", "D", false, 175);

        // Session 5 (Math Integration — 40% score): 8 correct, 12 wrong
        String session5 = "00000000-0000-0000-0000-000000000034";
        seedAnswer(session5, "q1", "C", "A", false, 195);
        seedAnswer(session5, "q2", "B", "B", true,   45);
        seedAnswer(session5, "q3", "A", "C", false, 230);
        seedAnswer(session5, "q4", "D", "D", true,   60);
        seedAnswer(session5, "q5", "B", "A", false, 200);
    }

    private void seedAnswer(String sessionId, String qRef,
                            String userAns, String correctAns,
                            boolean isCorrect, int timeSecs) {
        jdbc.update("""
            INSERT INTO test_answers
              (test_session_id, question_reference, user_answer, correct_answer,
               is_correct, time_taken_secs)
            VALUES (?::uuid, ?, ?, ?, ?, ?)
            ON CONFLICT DO NOTHING
            """, sessionId, qRef, userAns, correctAns, isCorrect, timeSecs);
    }

    // ── Concept Graph Nodes ──────────────────────────────────────────────────

    private void seedConceptGraphNodes() {
        seedGraphNode("jee-physics-thermodynamics", "First Law of Thermodynamics", 0.45);
        seedGraphNode("jee-physics-thermodynamics", "Entropy",                     0.30);
        seedGraphNode("jee-physics-thermodynamics", "Gibbs Free Energy",           0.25);
        seedGraphNode("jee-physics-thermodynamics", "Carnot Engine",               0.20);

        seedGraphNode("jee-chemistry-organic-goc",  "Inductive Effect",            0.35);
        seedGraphNode("jee-chemistry-organic-goc",  "Mesomeric Effect",            0.25);
        seedGraphNode("jee-chemistry-organic-goc",  "Hyperconjugation",            0.15);
        seedGraphNode("jee-chemistry-organic-goc",  "Carbocation Stability",       0.20);

        seedGraphNode("jee-math-integration",       "Integration by Parts",        0.40);
        seedGraphNode("jee-math-integration",       "Substitution Method",         0.60);
        seedGraphNode("jee-math-integration",       "Definite Integrals",          0.55);
        seedGraphNode("jee-math-integration",       "LIATE Rule",                  0.35);

        seedGraphNode("jee-physics-kinematics",     "Equations of Motion",         0.90);
        seedGraphNode("jee-physics-kinematics",     "Projectile Motion",           0.85);
        seedGraphNode("jee-physics-kinematics",     "Relative Motion",             0.80);
    }

    private void seedGraphNode(String topicId, String concept, double mastery) {
        jdbc.update("""
            INSERT INTO concept_graph_nodes (user_id, topic_id, concept, mastery)
            VALUES (?::uuid, ?, ?, ?)
            ON CONFLICT (user_id, topic_id, concept) DO NOTHING
            """, USER_ID, topicId, concept, mastery);
    }

    // ── Community Data ───────────────────────────────────────────────────────

    private void seedCommunityData() {
        String threadId = "00000000-0000-0000-0000-000000000050";
        String ans1Id   = "00000000-0000-0000-0000-000000000051";

        jdbc.update("""
            INSERT INTO community_threads
              (id, user_id, exam_id, subject_id, title, body,
               upvote_count, answer_count, is_resolved)
            VALUES (?::uuid, ?::uuid, 'jee', 'chemistry',
                    'How to approach JEE Advanced Organic Chemistry?',
                    'I am struggling with GOC especially inductive and mesomeric effects. Which textbook is best for JEE Advanced organic?',
                    7, 2, true)
            ON CONFLICT (id) DO NOTHING
            """, threadId, USER_ID);

        jdbc.update("""
            INSERT INTO community_answers
              (id, thread_id, user_id, body, is_mentor_answer, upvote_count, is_accepted)
            VALUES (?::uuid, ?::uuid, ?::uuid,
                    'Start with NCERT and then move to MS Chauhan. Focus on understanding the electron-donating/withdrawing nature first before memorizing effects. Inductive effect is distance-dependent while mesomeric operates through conjugation.',
                    true, 12, true)
            ON CONFLICT (id) DO NOTHING
            """, ans1Id, threadId, MENTOR_ID);

        jdbc.update("""
            INSERT INTO community_answers
              (thread_id, user_id, body, is_mentor_answer, upvote_count, is_accepted)
            VALUES (?::uuid, ?::uuid,
                    'Also try Himanshu Pandey for practice. The GOC chapter in JD Lee has great explanations too.',
                    false, 3, false)
            ON CONFLICT DO NOTHING
            """, threadId, USER_ID);
    }

    // ── Mentor Data ──────────────────────────────────────────────────────────

    private void seedMentorData() {
        jdbc.update("""
            INSERT INTO users (id, email, password_hash, full_name, role,
              is_email_verified, onboarding_complete)
            VALUES (?::uuid,
                    'mentor@prepcreatine.demo',
                    '$2a$12$demoHashMentor000000000000000000000000000000000000000',
                    'Dr. Priya Mehta', 'MENTOR', true, true)
            ON CONFLICT (id) DO NOTHING
            """, MENTOR_ID);

        jdbc.update("""
            INSERT INTO mentor_student_links (mentor_id, student_id, mentor_code)
            VALUES (?::uuid, ?::uuid, 'PRI-DEMO99')
            ON CONFLICT (mentor_id, student_id) DO NOTHING
            """, MENTOR_ID, USER_ID);

        jdbc.update("""
            INSERT INTO mentor_notes (mentor_id, student_id, content)
            VALUES (?::uuid, ?::uuid,
                    'Arjun is doing well in Physics but needs to focus more on Organic Chemistry, particularly GOC. Recommend 30 mins daily on reaction mechanisms before the next full mock. Score trend is improving — keep it up!')
            ON CONFLICT (mentor_id, student_id) DO UPDATE SET content = EXCLUDED.content
            """, MENTOR_ID, USER_ID);
    }

    // ── Daily Plan ───────────────────────────────────────────────────────────

    public void seedDailyPlan() {
        String sessionsJson = """
            [
              {"type":"review","topicId":"jee-physics-work-energy",
               "topicName":"Work, Energy and Power","subjectId":"physics",
               "subjectName":"Physics","durationMins":20,
               "reason":"Due for spaced repetition — last reviewed 8 days ago"},
              {"type":"review","topicId":"jee-physics-circular-motion",
               "topicName":"Circular Motion","subjectId":"physics",
               "subjectName":"Physics","durationMins":20,
               "reason":"Due for spaced repetition — 6-day review interval"},
              {"type":"review","topicId":"jee-chemistry-equilibrium",
               "topicName":"Chemical Equilibrium","subjectId":"chemistry",
               "subjectName":"Chemistry","durationMins":25,
               "reason":"Weak area — due for spaced repetition review"},
              {"type":"practice","topicId":"jee-chemistry-organic-goc",
               "topicName":"General Organic Chemistry","subjectId":"chemistry",
               "subjectName":"Chemistry","durationMins":30,
               "reason":"Weak topic (45% score) — targeted practice drill ready"},
              {"type":"new","topicId":"jee-math-probability",
               "topicName":"Probability","subjectId":"mathematics",
               "subjectName":"Mathematics","durationMins":25,
               "reason":"Next unstarted topic in your JEE Math syllabus"}
            ]
            """;

        jdbc.update("""
            INSERT INTO daily_plans
              (user_id, plan_date, total_minutes, sessions_json, motivation_msg)
            VALUES (?::uuid, CURRENT_DATE, 120, ?::jsonb,
                    'With 60 days to JEE, finishing Organic GOC this week keeps you exactly on track — your Physics scores are already strong.')
            ON CONFLICT (user_id, plan_date) DO UPDATE SET
              sessions_json = EXCLUDED.sessions_json,
              motivation_msg = EXCLUDED.motivation_msg
            """, USER_ID, sessionsJson);
    }

    // ── Notifications ────────────────────────────────────────────────────────

    private void seedNotifications() {
        seedNotif("streak_milestone",
                  "14-day study streak! 🔥",
                  "You've studied 14 days in a row. Keep the momentum going!",
                  null, false);
        seedNotif("mentor_note",
                  "Dr. Priya Mehta left you a note",
                  "Arjun is doing well in Physics but needs to focus on...",
                  null, false);
        seedNotif("test_result",
                  "New targeted drill ready",
                  "3 new questions on General Organic Chemistry based on your recent test performance. Practice them now!",
                  "/test/00000000-0000-0000-0000-000000000036", false);
        seedNotif("chapter_complete",
                  "Chapter completed!",
                  "You've completed all topics in Electrochemistry. Great work!",
                  null, true);
    }

    private void seedNotif(String type, String title, String body, String actionUrl, boolean isRead) {
        jdbc.update("""
            INSERT INTO notifications (user_id, type, title, body, action_url, is_read)
            VALUES (?::uuid, ?, ?, ?, ?, ?)
            ON CONFLICT DO NOTHING
            """, USER_ID, type, title, body, actionUrl, isRead);
    }

    // ── Questions ────────────────────────────────────────────────────────────

    private void seedQuestions() {
        // jee-chemistry-organic-goc level 2 MCQ
        seedQuestion("jee-chemistry-organic-goc", 2, "mcq",
            "The order of -I effect of the following groups is:\n" +
            "(A) -NO2 > -CN > -COOH > -F\n(B) -F > -CN > -NO2 > -COOH\n" +
            "(C) -NO2 > -CN > -F > -COOH\n(D) -F > -NO2 > -CN > -COOH",
            "A", "A",
            "The -I effect follows electronegativity and electron-withdrawing ability. -NO2 has strongest -I effect due to resonance-assisted induction. Order: -NO2 > -CN > -COOH > -F.");

        seedQuestion("jee-chemistry-organic-goc", 2, "mcq",
            "Which exhibits maximum +I effect?\n(A) -CH3  (B) -C2H5  (C) -C(CH3)3  (D) -CH2CH3",
            "C", "C",
            "The +I effect increases with chain branching. Tertiary groups are more electron-donating than secondary, which are more than primary alkyl groups.");

        seedQuestion("jee-chemistry-organic-goc", 2, "mcq",
            "Which group shows +M effect?\n(A) -NO2  (B) -CN  (C) -OH  (D) -CHO",
            "C", "C",
            "-OH has lone pairs that donate electron density into the ring via resonance (+M). -NO2, -CN, -CHO are all -M groups.");

        // jee-math-integration level 2
        seedQuestion("jee-math-integration", 2, "mcq",
            "∫x·e^x dx equals:\n(A) e^x(x-1)+C  (B) xe^x+C  (C) e^x(x+1)+C  (D) x^2·e^x/2+C",
            "A", "A",
            "By integration by parts: u=x, dv=e^x dx → du=dx, v=e^x. Result: xe^x - ∫e^x dx = xe^x - e^x + C = e^x(x-1) + C.");

        seedQuestion("jee-math-integration", 2, "mcq",
            "∫ln(x) dx equals:\n(A) 1/x+C  (B) x·ln(x)-x+C  (C) x·ln(x)+C  (D) ln(x)/x+C",
            "B", "B",
            "Integration by parts: u=ln(x), dv=dx → du=1/x dx, v=x. Result: x·ln(x) - ∫x·(1/x)dx = x·ln(x) - x + C.");

        // jee-physics-kinematics level 1
        seedQuestion("jee-physics-kinematics", 1, "mcq",
            "A body starts from rest with uniform acceleration 2 m/s². Distance in 5th second is:\n(A) 9m  (B) 25m  (C) 10m  (D) 5m",
            "A", "A",
            "Distance in nth second: Sn = u + a(2n-1)/2 = 0 + 2×(2×5-1)/2 = 9m.");

        seedQuestion("jee-physics-kinematics", 1, "mcq",
            "A ball thrown upward returns to ground. Time to reach max height vs total time ratio:\n(A) 1:1  (B) 1:2  (C) 2:1  (D) 1:3",
            "B", "B",
            "By symmetry of projectile motion, time to reach max height equals time to fall down. Total time = 2 × time up. Ratio = 1:2.");
    }

    private void seedQuestion(String topicId, int level, String type,
                              String questionText, String optionA, String correctAnswer,
                              String explanation) {
        jdbc.update("""
            INSERT INTO questions
              (topic_id, exam_id, level, question_type, question_text,
               correct_answer, explanation, is_ai_generated, source_ref)
            VALUES (?, 'jee', ?, ?, ?, ?, ?, false, 'JEE style demo question')
            ON CONFLICT DO NOTHING
            """, topicId, level, type, questionText, correctAnswer, explanation);
    }

    // ── AI Agent Demo Data ───────────────────────────────────────────────────

    /**
     * Seeds a realistic pre-computed learner profile and concept struggles
     * so that GET /api/analytics/agent-insights returns meaningful data
     * immediately at demo time, without waiting for the 2-hour agent cycle.
     */
    private void seedLearnerProfile() {
        // Upsert learner_profiles
        jdbc.update("""
            INSERT INTO learner_profiles
              (user_id, struggle_indicator, consistency_score,
               avg_time_per_correct, avg_time_per_wrong,
               total_questions_seen,
               weakness_pattern, strength_pattern, recommended_mode,
               learning_velocity, last_analyzed_at)
            VALUES (?::uuid, 0.52, 0.71, 42.0, 189.0, 87,
              'Struggles most with reaction mechanisms in Organic Chemistry (GOC) and definite integral evaluation; typically spends 3x as long on wrong answers, suggesting gaps in conceptual understanding rather than careless errors.',
              'Strong in Physics kinematics and coordinate geometry — consistently scores above 85% and solves problems in under 45 seconds.',
              'in_depth',
              0.64,
              NOW() - INTERVAL '30 minutes')
            ON CONFLICT (user_id) DO UPDATE SET
              weakness_pattern   = EXCLUDED.weakness_pattern,
              strength_pattern   = EXCLUDED.strength_pattern,
              recommended_mode   = EXCLUDED.recommended_mode,
              last_analyzed_at   = EXCLUDED.last_analyzed_at
            """, USER_ID);

        // Upsert concept_struggles (top struggles for agentic RAG enrichment)
        String[] concepts = {
            "reaction mechanisms",
            "definite integrals",
            "mesomeric effect",
            "inductive effect order",
            "entropy calculations",
            "carbocation stability",
            "integration by parts LIATE"
        };
        int[] counts = {7, 6, 5, 4, 4, 3, 3};
        for (int i = 0; i < concepts.length; i++) {
            final int idx = i;
            jdbc.update("""
                INSERT INTO concept_struggles (user_id, concept_tag, struggle_count, last_seen_at)
                VALUES (?::uuid, ?, ?, NOW() - ? * INTERVAL '1 day')
                ON CONFLICT (user_id, concept_tag) DO UPDATE SET
                  struggle_count = EXCLUDED.struggle_count
                """, USER_ID, concepts[idx], counts[idx], idx);
        }
    }

    /**
     * Seeds cross-session memory entries so the memory context shows up
     * in the first chat response during the demo.
     */
    private void seedMemoryEntries() {
        Object[][] memories = {
            {"misconception", "jee-chemistry-organic-goc",
             "reaction mechanisms, mesomeric effect",
             "Student confused +M and -M effects — confused -OH (electron donating) with -NO2 (electron withdrawing). Needs reinforcement on donor vs acceptor groups.",
             0.9, 14},
            {"concept_explanation", "jee-math-integration",
             "integration by parts, LIATE",
             "Explained LIATE rule for integration by parts. Student understood the concept but struggles to apply with trigonometric functions — needs more practice problems.",
             0.8, 7},
            {"difficulty_signal", "jee-physics-thermodynamics",
             "entropy, second law",
             "Student finds entropy conceptually difficult — expressed confusion about direction of spontaneous processes. Suggested visualizing disorder rather than memorizing formula.",
             0.75, 3}
        };

        for (Object[] m : memories) {
            jdbc.update("""
                INSERT INTO student_memory_entries
                  (user_id, memory_type, topic_id, concept_tags, summary,
                   importance_score, expires_at)
                VALUES (?::uuid, ?, ?, ?, ?, ?,
                        NOW() + INTERVAL '30 days')
                ON CONFLICT DO NOTHING
                """,
                USER_ID, m[0], m[1], m[2], m[3], m[4]);
        }
    }
}

