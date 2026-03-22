package com.prepcreatine.controller;

import com.prepcreatine.dto.request.*;
import com.prepcreatine.dto.response.*;
import com.prepcreatine.service.CommunityService;
import com.prepcreatine.service.community.RedditService;
import com.prepcreatine.util.SecurityUtil;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * Community Q&A endpoints per BSDD §14.
 *
 * GET    /api/community/threads
 * POST   /api/community/threads
 * GET    /api/community/threads/{id}
 * POST   /api/community/threads/{id}/answers
 * POST   /api/community/threads/{id}/upvote
 * POST   /api/community/answers/{id}/accept
 * POST   /api/community/answers/{id}/upvote
 */
@RestController
@RequestMapping("/api/community")
public class CommunityController {

    private final CommunityService communityService;
    private final RedditService redditService;

    public CommunityController(CommunityService communityService, RedditService redditService) {
        this.communityService = communityService;
        this.redditService = redditService;
    }

    @GetMapping("/threads")
    public ResponseEntity<PageResponse<CommunityThreadResponse>> listThreads(
        @RequestParam(required = false) String examId,
        @RequestParam(required = false) String subjectId,
        @RequestParam(required = false) String topicId,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        UUID currentUserId = SecurityUtil.getCurrentUserIdOptional();
        PageRequest pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(
            PageResponse.from(communityService.listThreads(examId, subjectId, topicId, currentUserId, pageable))
        );
    }

    @GetMapping("/threads/{id}")
    public ResponseEntity<CommunityThreadResponse> getThread(@PathVariable UUID id) {
        UUID currentUserId = SecurityUtil.getCurrentUserIdOptional();
        return ResponseEntity.ok(communityService.getThread(id, currentUserId));
    }

    @PostMapping("/threads")
    public ResponseEntity<CommunityThreadResponse> createThread(
        @Valid @RequestBody CreateThreadRequest req
    ) {
        CommunityThreadResponse response = communityService.createThread(req, SecurityUtil.getCurrentUserId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/threads/{id}/answers")
    public ResponseEntity<CommunityAnswerResponse> createAnswer(
        @PathVariable UUID id,
        @Valid @RequestBody CreateAnswerRequest req
    ) {
        CommunityAnswerResponse response = communityService.createAnswer(id, req, SecurityUtil.getCurrentUserId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/threads/{id}/upvote")
    public ResponseEntity<Map<String, String>> upvoteThread(@PathVariable UUID id) {
        communityService.toggleThreadUpvote(id, SecurityUtil.getCurrentUserId());
        return ResponseEntity.ok(Map.of("message", "Upvote toggled."));
    }

    @PostMapping("/answers/{id}/accept")
    public ResponseEntity<Map<String, String>> acceptAnswer(@PathVariable UUID id,
                                                             @RequestParam UUID threadId) {
        communityService.acceptAnswer(threadId, id, SecurityUtil.getCurrentUserId());
        return ResponseEntity.ok(Map.of("message", "Answer accepted."));
    }

    @PostMapping("/answers/{id}/upvote")
    public ResponseEntity<Map<String, String>> upvoteAnswer(@PathVariable UUID id) {
        communityService.toggleAnswerUpvote(id, SecurityUtil.getCurrentUserId());
        return ResponseEntity.ok(Map.of("message", "Upvote toggled."));
    }

    /**
     * GET /api/community/reddit-pulse?exam=jee
     * Returns trending Reddit posts. Returns empty list when Reddit is disabled.
     */
    @GetMapping("/reddit-pulse")
    public ResponseEntity<RedditPulseResponse> redditPulse(
        @RequestParam(defaultValue = "jee") String exam
    ) {
        return ResponseEntity.ok(redditService.getRedditPulse(exam));
    }
}
