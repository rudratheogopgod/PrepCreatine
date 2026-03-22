package com.prepcreatine.util;

import org.slf4j.Logger;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

/**
 * EvalLogger — writes to both the normal log AND logs/hackathon-eval.log simultaneously.
 *
 * Uses the HACKATHON_EVAL SLF4J marker so that logback-spring.xml can route these
 * messages to a clean, judge-readable file with no framework noise.
 *
 * Usage:
 *   EvalLogger.success(log, "MODULE-1 SM2", "topicId=xyz scheduled for nextReview=2026-03-30");
 *   EvalLogger.result(log, "MODULE-4 RAG", "chunks retrieved", 5);
 *   EvalLogger.agentAction(log, "PLANNER-AGENT", "Replan triggered — struggle detected");
 */
public final class EvalLogger {

    /** The marker that logback routes to hackathon-eval.log */
    public static final Marker HACKATHON_EVAL = MarkerFactory.getMarker("HACKATHON_EVAL");

    private EvalLogger() {}

    /** Use for any result a judge should clearly see as PASS / SUCCESS */
    public static void success(Logger log, String module, String message) {
        log.info(HACKATHON_EVAL, "✅ [{}] {} ✓", module, message);
    }

    /** Use for non-fatal issues or partial failures */
    public static void failure(Logger log, String module, String message) {
        log.warn(HACKATHON_EVAL, "❌ [{}] {} ✗", module, message);
    }

    /** Use for in-progress / intermediate steps */
    public static void step(Logger log, String module, String message) {
        log.info(HACKATHON_EVAL, "⏳ [{}] {}", module, message);
    }

    /** Use-for a labelled metric / data point (key: value pairs) */
    public static void result(Logger log, String module, String key, Object value) {
        log.info(HACKATHON_EVAL, "📊 [{}] {}: {}", module, key, value);
    }

    /** Use for autonomous agent decision / action events */
    public static void agentAction(Logger log, String agent, String action) {
        log.info(HACKATHON_EVAL, "🤖 [{}] AGENT ACTION: {}", agent, action);
    }

    /** Visual separator between logical test sections */
    public static void separator(Logger log) {
        log.info(HACKATHON_EVAL, "─────────────────────────────────────────");
    }
}
