package com.prepcreatine;

import com.prepcreatine.util.EvalLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.Banner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * PrepCreatine Backend Application Entry Point.
 * "Creatine for your exam prep" — brought to you by Rudra Agrawal.
 *
 * @EnableAsync      — required for @Async on email, source ingestion, agent tasks
 * @EnableCaching    — required for @Cacheable on YouTube, Reddit, roadmap, etc.
 * @EnableScheduling — required for @Scheduled LearnerAnalysisAgent (every 2h)
 */
@SpringBootApplication
@EnableAsync
@EnableCaching
@EnableScheduling
public class PrepCreatineApplication {

    private static final Logger log = LoggerFactory.getLogger(PrepCreatineApplication.class);

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(PrepCreatineApplication.class);
        app.setBannerMode(Banner.Mode.OFF); // We print our own banner below
        app.run(args);
    }

    /**
     * Fires after the application context is fully refreshed and all beans are ready.
     * Prints the startup banner + key config status to both console and hackathon-eval.log.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void onStartupComplete() {
        // ASCII art banner — printed to log output (visible in console + log file)
        log.info("\n" +
            "██████╗ ██████╗ ███████╗██████╗  ██████╗██████╗ ███████╗ █████╗ ████████╗██╗███╗   ██╗███████╗\n" +
            "██╔══██╗██╔══██╗██╔════╝██╔══██╗██╔════╝██╔══██╗██╔════╝██╔══██╗╚══██╔══╝██║████╗  ██║██╔════╝\n" +
            "██████╔╝██████╔╝█████╗  ██████╔╝██║     ██████╔╝█████╗  ███████║   ██║   ██║██╔██╗ ██║█████╗  \n" +
            "██╔═══╝ ██╔══██╗██╔══╝  ██╔═══╝ ██║     ██╔══██╗██╔══╝  ██╔══██║   ██║   ██║██║╚██╗██║██╔══╝  \n" +
            "██║     ██║  ██║███████╗██║     ╚██████╗██║  ██║███████╗██║  ██║   ██║   ██║██║ ╚████║███████╗\n" +
            "╚═╝     ╚═╝  ╚═╝╚══════╝╚═╝      ╚═════╝╚═╝  ╚═╝╚══════╝╚═╝  ╚═╝   ╚═╝   ╚═╝╚═╝  ╚═══╝╚══════╝\n" +
            "\n" +
            "  \"Creatine for your exam prep\" | DevClash2026 | NIT Raipur\n" +
            "  Brought to you by Rudra Agrawal\n");

        EvalLogger.separator(log);
        EvalLogger.success(log, "STARTUP", "PrepCreatine backend is READY");
        EvalLogger.result(log, "STARTUP", "Demo mode",
            Boolean.parseBoolean(System.getenv().getOrDefault("DEMO_MODE", "false")) ? "ENABLED" : "DISABLED");
        EvalLogger.result(log, "STARTUP", "Spring profile",
            System.getenv().getOrDefault("SPRING_PROFILES_ACTIVE", "default"));
        EvalLogger.result(log, "STARTUP", "Gemini API key configured",
            System.getenv("GEMINI_API_KEY") != null ? "YES" : "NO ⚠️");
        EvalLogger.result(log, "STARTUP", "YouTube API key configured",
            System.getenv("YOUTUBE_API_KEY") != null ? "YES" : "NO ⚠️");
        EvalLogger.separator(log);

        log.info("[Startup] PrepCreatine application started successfully ✓");
        log.info("[Startup] All Flyway migrations applied");
        log.info("[Startup] Startup self-test will run in ~3s...");
    }
}
