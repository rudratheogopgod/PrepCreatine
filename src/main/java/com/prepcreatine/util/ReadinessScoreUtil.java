package com.prepcreatine.util;

/**
 * Readiness score computation per BSDD §12.
 * Formula: round(completionPct * 0.50 + avgTestScore * 0.35 + daysRemainingFactor * 0.15)
 * Result clamped to [0, 100].
 */
public class ReadinessScoreUtil {

    private ReadinessScoreUtil() {}

    /**
     * @param completionPct       0.0-100.0 (syllabus completion percentage)
     * @param avgTestScore        0.0-100.0 (average test score percentage)
     * @param daysRemainingFactor 0.0-1.0   (1.0 = lots of time, 0.0 = exam imminent)
     * @return Readiness score 0-100
     */
    public static int compute(double completionPct, double avgTestScore, double daysRemainingFactor) {
        double score = (completionPct * 0.50)
                     + (avgTestScore  * 0.35)
                     + (daysRemainingFactor * 100.0 * 0.15);
        return (int) Math.round(Math.min(100.0, Math.max(0.0, score)));
    }
}
