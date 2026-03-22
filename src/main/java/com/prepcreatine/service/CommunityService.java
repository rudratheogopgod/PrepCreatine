package com.prepcreatine.service;

import com.prepcreatine.domain.*;
import com.prepcreatine.dto.request.*;
import com.prepcreatine.dto.response.*;
import com.prepcreatine.exception.*;
import com.prepcreatine.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Community Q&A service per BSDD §14.
 *
 * Features:
 * - Create/upvote/answer threads
 * - Accept answers (thread owner only)
 * - Auto-generate AI summary when answerCount > 5 (async)
 * - Mentor answers flagged with is_mentor_answer=true
 */
@Service
@Transactional
public class CommunityService {

    private static final Logger log = LoggerFactory.getLogger(CommunityService.class);

    private final CommunityThreadRepository  threadRepo;
    private final CommunityAnswerRepository  answerRepo;
    private final CommunityUpvoteRepository  upvoteRepo;
    private final UserRepository             userRepo;
    private final NotificationService        notifService;
    private final GeminiService              gemini;

    public CommunityService(CommunityThreadRepository threadRepo,
                            CommunityAnswerRepository answerRepo,
                            CommunityUpvoteRepository upvoteRepo,
                            UserRepository userRepo,
                            NotificationService notifService,
                            GeminiService gemini) {
        this.threadRepo   = threadRepo;
        this.answerRepo   = answerRepo;
        this.upvoteRepo   = upvoteRepo;
        this.userRepo     = userRepo;
        this.notifService = notifService;
        this.gemini       = gemini;
    }

    // ── Threads ───────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<CommunityThreadResponse> listThreads(String examId, String subjectId,
                                                      String topicId, UUID currentUserId,
                                                      Pageable pageable) {
        Page<CommunityThread> threads = threadRepo.findByFilters(examId, subjectId, topicId, pageable);
        return threads.map(t -> toThreadResponse(t, currentUserId));
    }

    @Transactional(readOnly = true)
    public CommunityThreadResponse getThread(UUID threadId, UUID currentUserId) {
        CommunityThread thread = findThread(threadId);
        return toThreadResponse(thread, currentUserId);
    }

    public CommunityThreadResponse createThread(CreateThreadRequest req, UUID userId) {
        CommunityThread thread = new CommunityThread();
        thread.setAuthorId(userId);
        thread.setExamId(req.examId());
        thread.setSubjectId(req.subjectId());
        thread.setTopicId(req.topicId());
        thread.setTitle(req.title().trim());
        thread.setBody(req.body());
        return toThreadResponse(threadRepo.save(thread), userId);
    }

    // ── Answers ───────────────────────────────────────────────────────────

    public CommunityAnswerResponse createAnswer(UUID threadId, CreateAnswerRequest req, UUID userId) {
        CommunityThread thread = findThread(threadId);
        User author = userRepo.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found."));

        CommunityAnswer answer = new CommunityAnswer();
        answer.setThreadId(threadId);
        answer.setAuthorId(userId);
        answer.setBody(req.body().trim());
        answer.setMentorAnswer("MENTOR".equals(author.getRole()));
        answer = answerRepo.save(answer);

        // Increment answer count
        thread.setAnswerCount(thread.getAnswerCount() + 1);
        threadRepo.save(thread);

        // Notify thread author
        if (!thread.getAuthorId().equals(userId)) {
            notifService.createNotification(thread.getAuthorId(),
                "new_answer", "New answer on your thread",
                author.getFullName() + " answered: " + thread.getTitle(),
                "/community/" + threadId);
        }

        // Trigger AI summary if answer count crosses threshold
        if (thread.getAnswerCount() > 5 && thread.getAiSummary() == null) {
            generateAiSummaryAsync(threadId);
        }

        return toAnswerResponse(answer, userId);
    }

    public void acceptAnswer(UUID threadId, UUID answerId, UUID userId) {
        CommunityThread thread = findThread(threadId);
        if (!thread.getAuthorId().equals(userId)) {
            throw new ForbiddenException("Only the thread author can accept answers.", userId);
        }

        // Un-accept any currently accepted answer
        answerRepo.findByThreadId(threadId).stream()
            .filter(CommunityAnswer::isAccepted)
            .forEach(a -> { a.setAccepted(false); answerRepo.save(a); });

        CommunityAnswer answer = answerRepo.findById(answerId)
            .orElseThrow(() -> new ResourceNotFoundException("Answer not found."));
        answer.setAccepted(true);
        answerRepo.save(answer);

        thread.setResolved(true);
        threadRepo.save(thread);
    }

    // ── Upvotes ───────────────────────────────────────────────────────────

    public void toggleThreadUpvote(UUID threadId, UUID userId) {
        CommunityThread thread = findThread(threadId);
        upvoteRepo.findByUserIdAndEntityIdAndEntityType(userId, threadId, "thread").ifPresentOrElse(
            upvote -> {
                upvoteRepo.delete(upvote);
                thread.setUpvoteCount(thread.getUpvoteCount() - 1);
            },
            () -> {
                CommunityUpvote up = new CommunityUpvote();
                up.setUserId(userId);
                up.setEntityId(threadId);
                up.setEntityType("thread");
                upvoteRepo.save(up);
                thread.setUpvoteCount(thread.getUpvoteCount() + 1);
            }
        );
        threadRepo.save(thread);
    }

    public void toggleAnswerUpvote(UUID answerId, UUID userId) {
        CommunityAnswer answer = answerRepo.findById(answerId)
            .orElseThrow(() -> new ResourceNotFoundException("Answer not found."));

        upvoteRepo.findByUserIdAndEntityIdAndEntityType(userId, answerId, "answer").ifPresentOrElse(
            upvote -> {
                upvoteRepo.delete(upvote);
                answer.setUpvoteCount(answer.getUpvoteCount() - 1);
            },
            () -> {
                CommunityUpvote up = new CommunityUpvote();
                up.setUserId(userId);
                up.setEntityId(answerId);
                up.setEntityType("answer");
                upvoteRepo.save(up);
                answer.setUpvoteCount(answer.getUpvoteCount() + 1);
            }
        );
        answerRepo.save(answer);
    }

    // ── Async AI Summary ──────────────────────────────────────────────────

    @Async
    protected void generateAiSummaryAsync(UUID threadId) {
        try {
            CommunityThread thread = findThread(threadId);
            List<String> answerBodies = answerRepo.findByThreadId(threadId)
                .stream()
                .map(CommunityAnswer::getBody)
                .toList();

            String prompt = "Thread: " + thread.getTitle() + "\n\n" +
                "Answers:\n" + String.join("\n---\n", answerBodies);

            String summary = gemini.generateContent(
                "Summarise the key insights from this community Q&A thread in 2-3 sentences. Be concise.",
                prompt);

            thread.setAiSummary(summary);
            threadRepo.save(thread);
        } catch (Exception e) {
            log.error("[Community] AI summary failed for threadId={}: {}", threadId, e.getMessage());
        }
    }

    // ── Mapping helpers ───────────────────────────────────────────────────

    private CommunityThreadResponse toThreadResponse(CommunityThread t, UUID currentUserId) {
        User author = userRepo.findById(t.getAuthorId()).orElse(null);
        UserSummaryResponse authorDto = author != null
            ? new UserSummaryResponse(author.getId(), author.getFullName(), author.getRole(),
              author.getCurrentStreak(), author.getReadinessScore()) : null;

        boolean upvotedByMe = currentUserId != null &&
            upvoteRepo.existsByUserIdAndEntityIdAndEntityType(currentUserId, t.getId(), "THREAD");

        return new CommunityThreadResponse(
            t.getId(), authorDto, t.getExamId(), t.getSubjectId(), t.getTopicId(),
            t.getTitle(), t.getBody(), t.getUpvoteCount(), t.getAnswerCount(),
            t.isResolved(), upvotedByMe, t.getAiSummary(), t.getCreatedAt(), t.getUpdatedAt());
    }

    private CommunityAnswerResponse toAnswerResponse(CommunityAnswer a, UUID currentUserId) {
        User author = userRepo.findById(a.getAuthorId()).orElse(null);
        UserSummaryResponse authorDto = author != null
            ? new UserSummaryResponse(author.getId(), author.getFullName(), author.getRole(),
              author.getCurrentStreak(), author.getReadinessScore()) : null;

        boolean upvotedByMe = currentUserId != null &&
            upvoteRepo.existsByUserIdAndEntityIdAndEntityType(currentUserId, a.getId(), "ANSWER");

        return new CommunityAnswerResponse(
            a.getId(), a.getThreadId(), authorDto, a.getBody(),
            a.getUpvoteCount(), a.isAccepted(), a.isMentorAnswer(),
            upvotedByMe, a.getCreatedAt(), a.getUpdatedAt());
    }

    private CommunityThread findThread(UUID id) {
        return threadRepo.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Thread not found."));
    }

    // Need to add the missing import
    private java.util.List<String> list(java.util.List<String> items) {
        return items;
    }
}
