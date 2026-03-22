package com.prepcreatine.service;

import com.prepcreatine.domain.UserTopicProgress;
import com.prepcreatine.repository.UserTopicProgressRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * SpacedRepetitionService — implements the SM-2 spaced repetition algorithm.
 *
 * SM-2 Algorithm:
 *   quality:    0–5 (0 = blackout, 5 = perfect recall in < 0.5s)
 *   easiness:   starts at 2.5, decreases for low quality, never < 1.3
 *   interval:   1, 6, then easiness × previous_interval (days)
 *   repetitions: count of reviews with quality >= 3
 *
 * Called from:
 *   - SyllabusController.updateTopicStatus → when topic marked 'done'
 *   - SyllabusController.updateSpacedRep  → when review marked complete via chat
 *   - QuizService.submitTest              → when test submitted (derive quality from score)
 */
@Service
@Transactional
public class SpacedRepetitionService {

    private static final Logger log = LoggerFactory.getLogger(SpacedRepetitionService.class);
    private static final BigDecimal MIN_EASINESS = new BigDecimal("1.3");

    private final UserTopicProgressRepository progressRepo;

    public SpacedRepetitionService(UserTopicProgressRepository progressRepo) {
        this.progressRepo = progressRepo;
    }

    /**
     * Apply an SM-2 review of a topic.
     *
     * @param userId  the student
     * @param topicId the topic being reviewed
     * @param quality 0–5 (callers should map scores to this scale)
     * @return updated UserTopicProgress with new nextReviewDate / intervalDays
     */
    public UserTopicProgress applyReview(UUID userId, String topicId, int quality) {
        if (quality < 0) quality = 0;
        if (quality > 5) quality = 5;

        UserTopicProgress p = progressRepo.findByUserIdAndTopicId(userId, topicId)
            .orElseGet(() -> {
                UserTopicProgress np = new UserTopicProgress();
                np.setUserId(userId);
                np.setTopicId(topicId);
                np.setExamId("jee");      // default; overridden if found
                np.setSubjectId("general");
                np.setStatus("done");
                return np;
            });

        // SM-2 core algorithm
        int reps         = p.getRepetitionCount();
        int intervalDays = p.getIntervalDays();
        BigDecimal ef    = p.getEasinessFactor();

        if (quality >= 3) {
            // Successful recall
            if (reps == 0)      intervalDays = 1;
            else if (reps == 1) intervalDays = 6;
            else                intervalDays = (int) Math.round(intervalDays * ef.doubleValue());
            reps++;
        } else {
            // Failed recall — restart sequence but keep easiness change
            reps         = 0;
            intervalDays = 1;
        }

        // Update easiness factor: EF' = EF + 0.1 - (5-q)*(0.08 + (5-q)*0.02)
        double delta = 0.1 - (5 - quality) * (0.08 + (5 - quality) * 0.02);
        ef = ef.add(BigDecimal.valueOf(delta)).max(MIN_EASINESS)
               .setScale(2, RoundingMode.HALF_UP);

        // Persist
        p.setRepetitionCount(reps);
        p.setIntervalDays(intervalDays);
        p.setEasinessFactor(ef);
        p.setNextReviewDate(LocalDate.now().plusDays(intervalDays));
        p.setLastReviewedAt(OffsetDateTime.now());
        if ("not_started".equals(p.getStatus()) || "in_progress".equals(p.getStatus())) {
            p.setStatus("done");
        }

        UserTopicProgress saved = progressRepo.save(p);
        log.debug("[SM-2] userId={}, topic={}, quality={}, newInterval={}, nextReview={}",
            userId, topicId, quality, intervalDays, saved.getNextReviewDate());
        return saved;
    }

    /**
     * Map a test score (0–100) to an SM-2 quality rating (0–5).
     * score >= 80 → 5, score >= 60 → 4, score >= 40 → 3,
     * score >= 20 → 2, score > 0 → 1, score = 0 → 0
     */
    public int scoreToQuality(double score) {
        if (score >= 80) return 5;
        if (score >= 60) return 4;
        if (score >= 40) return 3;
        if (score >= 20) return 2;
        if (score >  0)  return 1;
        return 0;
    }

    /**
     * Returns topics due for review today or earlier (for the ReviewDueWidget).
     * Only topics with status='done' and nextReviewDate <= today.
     */
    @Transactional(readOnly = true)
    public List<UserTopicProgress> getDueToday(UUID userId) {
        return progressRepo.findDueForReview(userId, LocalDate.now());
    }

    /**
     * Count of topics due (used for the sidebar badge).
     */
    @Transactional(readOnly = true)
    public long getDueTodayCount(UUID userId) {
        return progressRepo.countDueForReview(userId, LocalDate.now());
    }

    /**
     * Initialize SM-2 for a topic when it's first marked as 'done'.
     * Sets initial next review date to 1 day from now.
     */
    public void initializeForTopic(UUID userId, String topicId, String examId, String subjectId) {
        progressRepo.findByUserIdAndTopicId(userId, topicId).ifPresentOrElse(p -> {
            if (p.getNextReviewDate() == null) {
                // First time marking done — set up SM-2
                p.setStatus("done");
                p.setNextReviewDate(LocalDate.now().plusDays(1));
                p.setLastReviewedAt(OffsetDateTime.now());
                progressRepo.save(p);
            }
        }, () -> {
            UserTopicProgress p = new UserTopicProgress();
            p.setUserId(userId);
            p.setTopicId(topicId);
            p.setExamId(examId != null ? examId : "jee");
            p.setSubjectId(subjectId != null ? subjectId : "general");
            p.setStatus("done");
            p.setNextReviewDate(LocalDate.now().plusDays(1));
            p.setLastReviewedAt(OffsetDateTime.now());
            progressRepo.save(p);
        });
    }
}
