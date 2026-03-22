package com.prepcreatine.service;

import com.prepcreatine.domain.*;
import com.prepcreatine.dto.request.ChatMessageRequest;
import com.prepcreatine.dto.response.*;
import com.prepcreatine.exception.*;
import com.prepcreatine.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.util.*;

/**
 * Chat / RAG service per BSDD §7.
 *
 * Chat types:
 *   1. General AI chat (no source) — uses UserContext for topic-aware prompts
 *   2. Source-scoped RAG chat — retrieves relevant source chunks via pgvector
 *
 * Streaming: Returns Spring SseEmitter; Gemini reactive Flux is subscribed and
 * each token is emitted as an SSE event.
 *
 * Message persistence: Full assistant response stored after stream completes.
 */
@Service
@Transactional
public class ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatService.class);

    private final ConversationRepository conversation;
    private final MessageRepository      messageRepo;
    private final SourceRepository       sourceRepo;
    private final SourceChunkRepository  chunkRepo;
    private final UserContextRepository  contextRepo;
    private final GeminiService          gemini;

    public ChatService(ConversationRepository conversation,
                       MessageRepository messageRepo,
                       SourceRepository sourceRepo,
                       SourceChunkRepository chunkRepo,
                       UserContextRepository contextRepo,
                       GeminiService gemini) {
        this.conversation = conversation;
        this.messageRepo  = messageRepo;
        this.sourceRepo   = sourceRepo;
        this.chunkRepo    = chunkRepo;
        this.contextRepo  = contextRepo;
        this.gemini       = gemini;
    }

    // ── List conversations ─────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<ConversationResponse> list(UUID userId) {
        return conversation.findByUserIdOrderByUpdatedAtDesc(userId)
            .stream()
            .map(this::toConversationResponse)
            .toList();
    }

    @Transactional(readOnly = true)
    public List<MessageResponse> getMessages(UUID conversationId, UUID userId) {
        Conversation conv = findConversation(conversationId, userId);
        return messageRepo.findByConversationIdOrderByCreatedAtAsc(conv.getId())
            .stream()
            .map(this::toMessageResponse)
            .toList();
    }

    // ── SSE streaming chat ──────────────────────────────────────────────────

    /**
     * Creates or reuses a conversation, retrieves context, streams Gemini response,
     * and persists the complete response when done.
     */
    public SseEmitter streamChat(ChatMessageRequest req, UUID userId) {
        // 1. Resolve conversation
        Conversation conv;
        if (req.conversationId() != null) {
            conv = findConversation(req.conversationId(), userId);
        } else {
            conv = new Conversation();
            conv.setUserId(userId);
            conv.setTitle(truncateForTitle(req.message()));
            conversation.save(conv);
        }

        final UUID convId = conv.getId();

        // 2. Persist user message
        Message userMsg = new Message();
        userMsg.setConversationId(convId);
        userMsg.setRole("user");
        userMsg.setContent(req.message());
        messageRepo.save(userMsg);

        // 3. Build system prompt with context
        String systemPrompt = buildSystemPrompt(userId, conv);

        // 4. Build user prompt (with RAG chunks if source-scoped)
        String userPrompt = buildUserPrompt(req.message(), conv, userId);

        // 5. Create SSE emitter with 3 min timeout
        SseEmitter emitter = new SseEmitter(3 * 60 * 1000L);

        // 6. Subscribe to reactive stream on a background thread
        StringBuilder fullResponse = new StringBuilder();
        Flux<String> tokenFlux = gemini.streamGenerateContent(systemPrompt, userPrompt);

        tokenFlux.subscribe(
            token -> {
                try {
                    fullResponse.append(token);
                    emitter.send(SseEmitter.event()
                        .name("token")
                        .data(token));
                } catch (IOException e) {
                    emitter.completeWithError(e);
                }
            },
            error -> {
                log.error("[Chat] Streaming error for convId={}: {}", convId, error.getMessage());
                try {
                    emitter.send(SseEmitter.event().name("error").data("AI error occurred. Please retry."));
                } catch (IOException ignored) {}
                emitter.completeWithError(error);
            },
            () -> {
                // Persist complete assistant response
                Message assistantMsg = new Message();
                assistantMsg.setConversationId(convId);
                assistantMsg.setRole("assistant");
                assistantMsg.setContent(fullResponse.toString());
                messageRepo.save(assistantMsg);
                try {
                    emitter.send(SseEmitter.event().name("done").data("[DONE]"));
                } catch (IOException ignored) {}
                emitter.complete();
            }
        );

        return emitter;
    }

    // ── Delete conversation ────────────────────────────────────────────────

    public void deleteConversation(UUID conversationId, UUID userId) {
        Conversation conv = findConversation(conversationId, userId);
        messageRepo.deleteByConversationId(conv.getId());
        conversation.delete(conv);
    }

    // ── Private helpers ───────────────────────────────────────────────────

    private String buildSystemPrompt(UUID userId, Conversation conv) {
        StringBuilder sb = new StringBuilder();
        sb.append("You are PrepCreatine AI Tutor — an expert tutor helping a student prepare for competitive exams. ");
        sb.append("Respond with rich markdown. Include explanations, analogies, and practice tips. ");

        contextRepo.findByUserId(userId).ifPresent(ctx -> {
            if (ctx.getExamType() != null) {
                sb.append("The student is preparing for: ").append(ctx.getExamType()).append(". ");
            }
        });

        if (conv.getSourceId() != null) {
            sourceRepo.findById(conv.getSourceId()).ifPresent(src -> {
                sb.append("This conversation is scoped to the study material: '").append(src.getTitle()).append("'. ");
                sb.append("Answer only based on this material when possible.");
            });
        }

        return sb.toString();
    }

    private String buildUserPrompt(String message, Conversation conv, UUID userId) {
        if (conv.getSourceId() == null) {
            return message;
        }

        // RAG: embed the user message and retrieve top-k chunks
        try {
            float[] queryEmbedding = gemini.embedText(message);
            List<SourceChunk> chunks = chunkRepo.findTopKByCosineSimilarity(
                conv.getSourceId(), queryEmbedding, 5);

            if (chunks.isEmpty()) return message;

            StringBuilder sb = new StringBuilder();
            sb.append("Relevant context from the study material:\n\n");
            chunks.forEach(c -> sb.append("---\n").append(c.getContent()).append("\n"));
            sb.append("\n---\n\nStudent question: ").append(message);
            return sb.toString();
        } catch (Exception e) {
            log.warn("[Chat] RAG retrieval failed for sourceId={}: {}", conv.getSourceId(), e.getMessage());
            return message;
        }
    }

    private Conversation findConversation(UUID conversationId, UUID userId) {
        return conversation.findByIdAndUserId(conversationId, userId)
            .orElseThrow(() -> new ResourceNotFoundException("Conversation not found."));
    }

    private String truncateForTitle(String message) {
        return message.length() > 60 ? message.substring(0, 57) + "..." : message;
    }

    private ConversationResponse toConversationResponse(Conversation c) {
        return new ConversationResponse(
            c.getId(), c.getTitle(), c.getSourceId(), null,
            c.getCreatedAt(), c.getUpdatedAt());
    }

    private MessageResponse toMessageResponse(Message m) {
        return new MessageResponse(
            m.getId(), m.getConversationId(), m.getRole(),
            m.getContent(), m.getConceptMap(),
            m.getYoutubeIds() != null ? List.of(m.getYoutubeIds()) : List.of(),
            null, m.getCreatedAt());
    }
}
