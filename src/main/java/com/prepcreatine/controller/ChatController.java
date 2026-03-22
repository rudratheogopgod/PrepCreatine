package com.prepcreatine.controller;

import com.prepcreatine.dto.request.ChatMessageRequest;
import com.prepcreatine.dto.response.*;
import com.prepcreatine.service.ChatService;
import com.prepcreatine.service.SessionLoggingService;
import com.prepcreatine.util.SecurityUtil;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Chat / conversation endpoints per BSDD §7.
 *
 * GET    /api/conversations
 * GET    /api/conversations/{id}/messages
 * POST   /api/chat                              — SSE streaming
 * DELETE /api/conversations/{id}
 */
@RestController
public class ChatController {

    private final ChatService           chatService;
    private final SessionLoggingService sessionLog;

    public ChatController(ChatService chatService, SessionLoggingService sessionLog) {
        this.chatService = chatService;
        this.sessionLog  = sessionLog;
    }

    @GetMapping("/api/conversations")
    public ResponseEntity<List<ConversationResponse>> listConversations() {
        return ResponseEntity.ok(chatService.list(SecurityUtil.getCurrentUserId()));
    }

    @GetMapping("/api/conversations/{id}/messages")
    public ResponseEntity<List<MessageResponse>> getMessages(@PathVariable UUID id) {
        return ResponseEntity.ok(chatService.getMessages(id, SecurityUtil.getCurrentUserId()));
    }

    /**
     * SSE streaming endpoint — returns text/event-stream.
     * SseEmitter created inside ChatService and returned directly.
     * Each Gemini token emitted as an SSE 'token' event.
     * Final 'done' event signals completion.
     */
    @PostMapping(value = "/api/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chat(@Valid @RequestBody ChatMessageRequest req) {
        UUID userId = SecurityUtil.getCurrentUserId();
        sessionLog.log(userId, 1); // Count 1 study minute per chat interaction
        return chatService.streamChat(req, userId);
    }

    @DeleteMapping("/api/conversations/{id}")
    public ResponseEntity<Map<String, String>> deleteConversation(@PathVariable UUID id) {
        chatService.deleteConversation(id, SecurityUtil.getCurrentUserId());
        return ResponseEntity.ok(Map.of("message", "Conversation deleted."));
    }
}
