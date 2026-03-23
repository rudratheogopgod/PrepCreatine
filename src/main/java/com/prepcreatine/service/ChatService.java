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
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Chat / Agentic RAG service.
 *
 * Key upgrades from the base implementation:
 *  1. Agentic RAG: query enriched with student's top struggle concepts
 *  2. Memory injection: cross-session memory block in every prompt
 *  3. Learner profile injection: behavioral stats shape AI tone and depth
 *  4. Post-stream: conceptTagsDetected extracted → memory + struggles updated
 */
@Service
@Transactional
public class ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatService.class);

    private final ConversationRepository    conversation;
    private final MessageRepository         messageRepo;
    private final SourceRepository          sourceRepo;
    private final SourceChunkRepository     chunkRepo;
    private final UserContextRepository     contextRepo;
    private final GeminiService             gemini;
    private final LearnerProfileService     learnerProfileService;
    private final StudentMemoryService      memoryService;
    private final ConceptStruggleRepository conceptStruggleRepo;

    public ChatService(ConversationRepository conversation,
                       MessageRepository messageRepo,
                       SourceRepository sourceRepo,
                       SourceChunkRepository chunkRepo,
                       UserContextRepository contextRepo,
                       GeminiService gemini,
                       LearnerProfileService learnerProfileService,
                       StudentMemoryService memoryService,
                       ConceptStruggleRepository conceptStruggleRepo) {
        this.conversation         = conversation;
        this.messageRepo          = messageRepo;
        this.sourceRepo           = sourceRepo;
        this.chunkRepo            = chunkRepo;
        this.contextRepo          = contextRepo;
        this.gemini               = gemini;
        this.learnerProfileService = learnerProfileService;
        this.memoryService        = memoryService;
        this.conceptStruggleRepo  = conceptStruggleRepo;
    }

    // ── List conversations ─────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<ConversationResponse> list(UUID userId) {
        return conversation.findByUserIdOrderByUpdatedAtDesc(userId)
            .stream().map(this::toConversationResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<MessageResponse> getMessages(UUID conversationId, UUID userId) {
        Conversation conv = findConversation(conversationId, userId);
        return messageRepo.findByConversationIdOrderByCreatedAtAsc(conv.getId())
            .stream().map(this::toMessageResponse).toList();
    }

    // ── Agentic SSE streaming chat ─────────────────────────────────────────

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
        userMsg.setUserId(userId);
        userMsg.setRole("user");
        userMsg.setContent(req.message());
        messageRepo.save(userMsg);

        // 3. Load context
        UserContext ctx = contextRepo.findByUserId(userId).orElse(null);
        String examId = ctx != null && ctx.getExamType() != null
            ? ctx.getExamType() : "jee";

        // 4. Get learner profile summary (drives AI tone, depth, retrieval)
        String profileSummary = learnerProfileService.buildProfileSummary(userId);

        // 5. Get top struggle concepts for query enrichment
        List<ConceptStruggle> struggles = conceptStruggleRepo
            .findTopByUserIdOrderByStruggleCountDesc(userId, 3);
        List<String> struggleTags = struggles.stream()
            .map(ConceptStruggle::getConceptTag).collect(Collectors.toList());

        // 6. Get cross-session memory context — use first weak topic as current topic hint
        String currentTopicId = ctx != null && ctx.getWeakTopics() != null && ctx.getWeakTopics().length > 0
            ? ctx.getWeakTopics()[0] : null;
        String memoryContext = memoryService.buildMemoryContext(userId, currentTopicId);

        // 7. Build history
        List<Message> history = messageRepo
            .findByConversationIdOrderByCreatedAtAsc(convId)
            .stream().limit(10).collect(Collectors.toList());

        // 8. Build agentic system prompt (profile + memory injected)
        String systemPrompt = buildAgenticSystemPrompt(
            userId, conv, ctx, profileSummary, memoryContext, struggles);

        // 9. Build user prompt with enriched RAG retrieval
        String userPrompt = buildAgenticUserPrompt(
            req.message(), conv, userId, struggleTags, profileSummary);

        // 10. Collect full stream, strip ---JSON--- block, then emit clean tokens
        SseEmitter emitter = new SseEmitter(3 * 60 * 1000L);
        Flux<String> tokenFlux = gemini.streamGenerateContent(systemPrompt, userPrompt);

        // Collect the full response first, THEN strip JSON and stream clean text.
        // This avoids leaking the ---JSON--- metadata block to the user.
        tokenFlux
            .collectList()
            .doOnSuccess(tokens -> {
                String fullRaw = String.join("", tokens);

                // ── Strip ---JSON--- block ────────────────────────────────────
                String cleanText = fullRaw;
                String extractedJson = null;
                int jsonStart = fullRaw.indexOf("---JSON---");
                if (jsonStart != -1) {
                    // Everything before the first ---JSON--- marker is the human text
                    cleanText = fullRaw.substring(0, jsonStart).trim();
                    int jsonEnd = fullRaw.indexOf("---JSON---", jsonStart + 10);
                    if (jsonEnd != -1) {
                        extractedJson = fullRaw.substring(jsonStart + 10, jsonEnd).trim();
                    }
                    log.debug("[Chat] Stripped JSON block ({} chars) from response",
                        fullRaw.length() - cleanText.length());
                }

                // ── Save clean text (without JSON) to DB ──────────────────────
                Message assistantMsg = new Message();
                assistantMsg.setConversationId(convId);
                assistantMsg.setUserId(userId);
                assistantMsg.setRole("assistant");
                assistantMsg.setContent(cleanText);
                messageRepo.save(assistantMsg);

                // ── Post-stream: extract concept tags for memory + struggles ──
                String textForAnalysis = extractedJson != null ? extractedJson : cleanText;
                List<String> detectedTags = memoryService.parseConceptTagsFromResponse(textForAnalysis);
                memoryService.extractMemoriesFromChat(
                    userId, req.message(), cleanText, currentTopicId, detectedTags);

                learnerProfileService.updateFromStudySession(
                    userId, 5, detectedTags.isEmpty() ? List.of() : detectedTags);

                if (!detectedTags.isEmpty() && !struggles.isEmpty()) {
                    Set<String> activeStruggleTags = struggles.stream()
                        .map(ConceptStruggle::getConceptTag).collect(Collectors.toSet());
                    detectedTags.stream()
                        .filter(activeStruggleTags::contains)
                        .forEach(tag -> {
                            try {
                                conceptStruggleRepo.incrementStruggle(userId, tag);
                            } catch (Exception e) {
                                log.debug("[Chat] Could not increment struggle: {}", e.getMessage());
                            }
                        });
                }

                // ── Stream clean text as tokens to frontend ───────────────────
                // Chunk at word boundaries so the UI feels natural
                String[] words = cleanText.split("(?<=\\s)|(?=\\s)");
                for (String word : words) {
                    try {
                        emitter.send(SseEmitter.event().name("token").data(word));
                    } catch (IOException e) {
                        emitter.completeWithError(e);
                        return;
                    }
                }

                try {
                    emitter.send(SseEmitter.event().name("done").data("[DONE]"));
                } catch (IOException ignored) {}
                emitter.complete();
            })
            .doOnError(error -> {
                log.error("[Chat] Streaming error for convId={}: {}", convId, error.getMessage());
                try {
                    emitter.send(SseEmitter.event().name("error")
                        .data("AI error occurred. Please retry."));
                } catch (IOException ignored) {}
                emitter.completeWithError(error);
            })
            .subscribe();

        return emitter;
    }

    // ── Delete conversation ────────────────────────────────────────────────

    public void deleteConversation(UUID conversationId, UUID userId) {
        Conversation conv = findConversation(conversationId, userId);
        messageRepo.deleteByConversationId(conv.getId());
        conversation.delete(conv);
    }

    // ── Agentic prompt builders ───────────────────────────────────────────

    /**
     * Builds the system prompt with learner profile + cross-session memory injected.
     * This is what makes PrepCreatine an AI agent, not just a chatbot.
     */
    private String buildAgenticSystemPrompt(UUID userId,
                                             Conversation conv,
                                             UserContext ctx,
                                             String profileSummary,
                                             String memoryContext,
                                             List<ConceptStruggle> struggles) {
        StringBuilder sb = new StringBuilder();
        sb.append("You are PrepCreatine, an AI study tutor for competitive exam preparation.\n");
        sb.append("Personality: encouraging, precise, adaptive. Never condescending.\n");
        sb.append("\"Creatine for your exam prep\" — you make students stronger.\n\n");

        // Inject exam context
        if (ctx != null) {
            if (ctx.getExamType() != null)
                sb.append("EXAM: ").append(ctx.getExamType().toUpperCase()).append("\n");
            if (ctx.getExamDate() != null) {
                long days = ChronoUnit.DAYS.between(LocalDate.now(), ctx.getExamDate());
                sb.append("DAYS UNTIL EXAM: ").append(days).append("\n");
            }
            if (ctx.getStudyMode() != null)
                sb.append("STUDY MODE: ").append(ctx.getStudyMode()).append("\n");
        }

        // ── CORE AGENT FEATURE: Inject persistent behavioral memory ─────────
        sb.append("\nBEHAVIORAL MEMORY (persistent learner profile):\n");
        sb.append(profileSummary).append("\n");

        // Inject coaching instructions derived from profile
        sb.append("\nCOACHING INSTRUCTIONS:\n");
        sb.append(buildCoachingInstructions(profileSummary)).append("\n");

        // Inject cross-session memory (what was studied before)
        if (memoryContext != null && !memoryContext.isBlank()) {
            sb.append("\n").append(memoryContext).append("\n");
        }

        // Source scoping
        if (conv.getSourceId() != null) {
            sourceRepo.findById(conv.getSourceId()).ifPresent(src ->
                sb.append("\nSOURCE SCOPE: Answer primarily from material: '")
                  .append(src.getTitle()).append("'.\n"));
        }

        sb.append("""

            After your explanation, append this structured JSON block:
            ---JSON---
            {
              "conceptMap": {
                "nodes": [{"id":"string","label":"string","type":"concept|formula|example"}],
                "edges": [{"source":"string","target":"string","label":"string"}]
              },
              "youtubeSearchTerm": "specific YouTube search term",
              "practiceQuestions": [{"question":"string","answer":"string"}],
              "relatedTopicIds": ["topicId1"],
              "groundedIn": ["source title 1"],
              "conceptTagsDetected": ["tag1","tag2"]
            }
            ---JSON---
            conceptTagsDetected: list concepts from: rate_of_change, 3d_spatial,
            equilibrium_reasoning, electron_movement, integration, energy_conservation
            """);

        return sb.toString();
    }

    /**
     * Builds user prompt with agentic RAG:
     * 1. Enriches the query with top struggle concepts before embedding
     * 2. Retrieves chunks and re-ranks by struggle relevance
     */
    private String buildAgenticUserPrompt(String message,
                                           Conversation conv,
                                           UUID userId,
                                           List<String> struggleTags,
                                           String profileSummary) {
        if (conv.getSourceId() == null) {
            return message;
        }

        try {
            // Enrich query embedding with struggle context (agentic query rewriting)
            String enrichedQuery = message;
            if (!struggleTags.isEmpty()) {
                enrichedQuery = message + " " + String.join(" ", struggleTags);
            }
            // If student is struggling heavily, add foundational terms
            if (profileSummary.contains("struggle_indicator")) {
                try {
                    double si = Double.parseDouble(
                        profileSummary.replaceAll(".*struggle_indicator=(\\d+\\.\\d+).*", "$1"));
                    if (si > 0.6) enrichedQuery += " fundamental concepts basic explanation";
                } catch (NumberFormatException ignored) {}
            }

            float[] queryEmbedding = gemini.embedText(enrichedQuery);
            List<SourceChunk> chunks = chunkRepo.findTopKByCosineSimilarity(
                conv.getSourceId(), queryEmbedding, 5);

            if (chunks.isEmpty()) return message;

            // Re-rank: chunks matching struggle tags go first
            if (!struggleTags.isEmpty()) {
                Set<String> tagSet = new HashSet<>(struggleTags);
                chunks.sort((a, b) -> {
                    String ta = a.getContent() != null ? a.getContent().toLowerCase() : "";
                    String tb = b.getContent() != null ? b.getContent().toLowerCase() : "";
                    boolean am = tagSet.stream()
                        .anyMatch(t -> ta.contains(t.replace("_", " ")));
                    boolean bm = tagSet.stream()
                        .anyMatch(t -> tb.contains(t.replace("_", " ")));
                    if (am && !bm) return -1;
                    if (!am && bm) return 1;
                    return 0;
                });
            }

            StringBuilder sb = new StringBuilder();
            sb.append("SOURCE-GROUNDED CONTEXT:\n\n");
            chunks.forEach(c -> sb.append("---\n").append(c.getContent()).append("\n"));
            sb.append("\n---\n\nStudent question: ").append(message);
            return sb.toString();

        } catch (Exception e) {
            log.warn("[Chat] Agentic RAG failed for sourceId={}: {}",
                conv.getSourceId(), e.getMessage());
            return message;
        }
    }

    /** Generates dynamic coaching instructions from the profile summary string. */
    private String buildCoachingInstructions(String profileSummary) {
        try {
            if (profileSummary.contains("struggle_indicator")) {
                double si = Double.parseDouble(
                    profileSummary.replaceAll(".*struggle_indicator=(\\d+\\.\\d+).*", "$1"));
                if (si > 0.6)
                    return "Student is struggling heavily. Use more analogies, real-world examples. " +
                           "Break proofs into numbered steps. Start with the simplest version.";
                if (si < 0.3)
                    return "Student is performing well. Use precise language, include exam " +
                           "tricks and common traps. Challenge with edge cases.";
            }
            if (profileSummary.contains("consistency=")) {
                double cons = Double.parseDouble(
                    profileSummary.replaceAll(".*consistency=(\\d+)%.*", "$1"));
                if (cons < 30)
                    return "Student studies inconsistently. Keep explanations self-contained. " +
                           "End with a memorable one-line summary.";
            }
        } catch (NumberFormatException ignored) {}
        return "Student is on track. Balanced explanations with practical exam focus.";
    }

    // ── Private helpers ───────────────────────────────────────────────────

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
