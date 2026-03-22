package com.prepcreatine.util;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Date utilities for readiness score computation.
 */
public class DateUtil {

    private DateUtil() {}

    /**
     * Days until exam from today. Returns 0 if exam date is in the past.
     */
    public static long daysUntilExam(LocalDate examDate) {
        if (examDate == null) return 365;
        long days = ChronoUnit.DAYS.between(LocalDate.now(), examDate);
        return Math.max(0, days);
    }

    /**
     * Normalised factor 0.0–1.0 based on days remaining.
     * More days remaining = higher factor (student has time to improve).
     * <7 days = 0.0 (pressure), >180 days = 1.0 (relaxed).
     */
    public static double daysRemainingFactor(long daysRemaining) {
        if (daysRemaining <= 7)   return 0.0;
        if (daysRemaining >= 180) return 1.0;
        return (daysRemaining - 7.0) / (180.0 - 7.0);
    }
}
