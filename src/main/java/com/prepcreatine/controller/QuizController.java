package com.prepcreatine.controller;

import com.prepcreatine.dto.request.SubmitTestRequest;
import com.prepcreatine.dto.response.TestSessionResponse;
import com.prepcreatine.service.QuizService;
import com.prepcreatine.service.SessionLoggingService;
import com.prepcreatine.util.SecurityUtil;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Quiz / test endpoints per BSDD §13.
 *
 * POST /api/tests/start
 * POST /api/tests/{id}/submit
 * GET  /api/tests
 * GET  /api/tests/{id}
 */
@RestController
@RequestMapping("/api/tests")
public class QuizController {

    private final QuizService           quizService;
    private final SessionLoggingService sessionLog;

    public QuizController(QuizService quizService, SessionLoggingService sessionLog) {
        this.quizService = quizService;
        this.sessionLog  = sessionLog;
    }

    @PostMapping("/start")
    public ResponseEntity<TestSessionResponse> startTest(
        @RequestParam String examId,
        @RequestParam(required = false) String subjectId,
        @RequestParam(required = false) String topicId,
        @RequestParam(defaultValue = "10") int questions,
        @RequestParam(defaultValue = "30") int timeLimitMins
    ) {
        UUID userId = SecurityUtil.getCurrentUserId();
        sessionLog.log(userId, 5); // 5 mins for starting a test
        TestSessionResponse response = quizService.startTest(
            userId, examId, subjectId, topicId, questions, timeLimitMins);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/{id}/submit")
    public ResponseEntity<TestSessionResponse> submitTest(
        @PathVariable UUID id,
        @Valid @RequestBody SubmitTestRequest req
    ) {
        UUID userId = SecurityUtil.getCurrentUserId();
        // Log study time based on time limit
        sessionLog.log(userId, 0); // Duration tracked per test session already
        return ResponseEntity.ok(quizService.submitTest(req, userId));
    }

    @GetMapping
    public ResponseEntity<List<TestSessionResponse>> listTests() {
        return ResponseEntity.ok(quizService.listForUser(SecurityUtil.getCurrentUserId()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TestSessionResponse> getTest(@PathVariable UUID id) {
        return ResponseEntity.ok(quizService.getById(id, SecurityUtil.getCurrentUserId()));
    }
}
