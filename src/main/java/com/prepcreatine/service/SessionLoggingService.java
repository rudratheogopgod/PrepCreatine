package com.prepcreatine.service;

import com.prepcreatine.domain.Session;
import com.prepcreatine.domain.User;
import com.prepcreatine.repository.SessionRepository;
import com.prepcreatine.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

/**
 * Session logging per BSDD §11 streak algorithm.
 *
 * Called @Async from controllers on every authenticated API request
 * so that request threads are never blocked by this logic.
 *
 * Algorithm:
 * 1. UPSERT session row for today (INSERT ... ON CONFLICT DO UPDATE)
 *    — adds studyMins to daily total
 * 2. Count consecutive days backwards from yesterday
 * 3. Update user.current_streak, user.longest_streak, user.total_days
 */
@Service
public class SessionLoggingService {

    private static final Logger log = LoggerFactory.getLogger(SessionLoggingService.class);

    private final SessionRepository  sessionRepo;
    private final UserRepository     userRepo;

    public SessionLoggingService(SessionRepository sessionRepo, UserRepository userRepo) {
        this.sessionRepo = sessionRepo;
        this.userRepo    = userRepo;
    }

    /**
     * Async call from any controller action that counts as a study session.
     * @param userId     authenticated user
     * @param studyMins  minutes to add for this action (e.g. 1 for chat message)
     */
    @Async
    @Transactional
    public void log(UUID userId, int studyMins) {
        try {
            LocalDate today = LocalDate.now();

            // UPSERT daily session
            Optional<Session> existing = sessionRepo.findByUserIdAndDate(userId, today);
            if (existing.isPresent()) {
                Session s = existing.get();
                s.setDurationMins(s.getDurationMins() + studyMins);
                sessionRepo.save(s);
            } else {
                Session s = new Session();
                s.setUserId(userId);
                s.setDate(today);
                s.setDurationMins(studyMins);
                sessionRepo.save(s);
            }

            // Recompute streak
            userRepo.findByIdAndIsActiveTrue(userId).ifPresent(user -> {
                recalcStreak(user);
                userRepo.save(user);
            });
        } catch (Exception e) {
            log.error("[SessionLog] Failed for userId={}: {}", userId, e.getMessage());
            // Swallow — do not interrupt user-facing request
        }
    }

    private void recalcStreak(User user) {
        // Count consecutive days from yesterday backwards
        LocalDate date  = LocalDate.now();
        int streak      = 0;
        int totalDays   = sessionRepo.countDistinctDaysByUserId(user.getId());

        // Walk back from today
        while (sessionRepo.existsByUserIdAndDate(user.getId(), date)) {
            streak++;
            date = date.minusDays(1);
        }

        user.setCurrentStreak(streak);
        user.setTotalDays(totalDays);
        if (streak > user.getLongestStreak()) {
            user.setLongestStreak(streak);
        }
    }
}
