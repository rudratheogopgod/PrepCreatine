package com.prepcreatine.service;

import com.prepcreatine.domain.StudentMemoryEntry;
import com.prepcreatine.repository.ConceptStruggleRepository;
import com.prepcreatine.repository.StudentMemoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * StudentMemoryService — Component E: Cross-Session Persistent Memory
 *
 * Extracts and stores memories from every AI chat exchange.
 * Provides buildMemoryContext() to inject past-session memories into prompts
 * so the AI "remembers" what it taught and what the student struggled with.
 *
 * Memory types:
 *   concept_explained  — AI explained a concept in this session
 *   misconception      — student had a misconception that was corrected
 *   difficulty_signal  — student expressed confusion ("I don't understand...")
 *   preferred_example  — student responded well to a specific example type
 */
@Service
@Transactional
public class StudentMemoryService {

    private static final Logger log = LoggerFactory.getLogger(StudentMemoryService.class);

    private final StudentMemoryRepository   memoryRepo;
    private final ConceptStruggleRepository conceptStruggleRepo;

    public StudentMemoryService(StudentMemoryRepository memoryRepo,
                                ConceptStruggleRepository conceptStruggleRepo) {
        this.memoryRepo          = memoryRepo;
        this.conceptStruggleRepo = conceptStruggleRepo;
    }

    /**
     * Extract and persist memories from a completed chat exchange.
     * Called asynchronously after the SSE stream completes in ChatService.
     */
    @Async
    public void extractMemoriesFromChat(UUID userId,
                                        String userMessage,
                                        String aiResponse,
                                        String topicId,
                                        List<String> conceptTags) {
        try {
            List<StudentMemoryEntry> newMemories = new ArrayList<>();

            // Memory 1: Concept was explained this session
            if (conceptTags != null && !conceptTags.isEmpty()) {
                for (String tag : conceptTags) {
                    StudentMemoryEntry mem = new StudentMemoryEntry();
                    mem.setUserId(userId);
                    mem.setMemoryType("concept_explained");
                    mem.setTopicId(topicId);
                    mem.setConcept(tag);
                    mem.setSummary("Student asked about " + tag +
                        (topicId != null ? " in topic " + topicId : "") +
                        ". AI explained it.");
                    mem.setImportance((short) 1);
                    mem.setExpiresAt(Instant.now().plus(30, ChronoUnit.DAYS));
                    newMemories.add(mem);
                }
            }

            // Memory 2: Detect difficulty signals from user's message
            String lower = userMessage != null ? userMessage.toLowerCase() : "";
            boolean expressedDifficulty =
                lower.contains("don't understand") || lower.contains("dont understand") ||
                lower.contains("confused") || lower.contains("explain again") ||
                lower.contains("help me") || lower.contains("samajh nahi") ||
                lower.contains("kaise") || lower.contains("why does") ||
                lower.contains("not getting") || lower.contains("samajh nahi aaya");

            if (expressedDifficulty && topicId != null) {
                StudentMemoryEntry mem = new StudentMemoryEntry();
                mem.setUserId(userId);
                mem.setMemoryType("difficulty_signal");
                mem.setTopicId(topicId);
                mem.setSummary("Student expressed confusion about " + topicId +
                    ". Message: '" +
                    (userMessage.length() > 100 ?
                        userMessage.substring(0, 100) + "..." : userMessage) + "'");
                mem.setImportance((short) 3); // high importance
                mem.setExpiresAt(Instant.now().plus(14, ChronoUnit.DAYS));
                newMemories.add(mem);

                // Also increment concept struggles for active struggle tags
                if (conceptTags != null) {
                    conceptTags.forEach(tag ->
                        conceptStruggleRepo.upsertStruggle(userId, topicId, tag));
                }
            }

            if (!newMemories.isEmpty()) {
                memoryRepo.saveAll(newMemories);
                log.debug("[Memory] Stored {} memories for userId={}", newMemories.size(), userId);
            }

        } catch (Exception e) {
            log.warn("[Memory] Extraction failed userId={}: {}", userId, e.getMessage());
        }
    }

    /**
     * Build the "STUDENT MEMORY" context block for injection into chat prompts.
     * Topic-specific memories are listed first; other recent memories second.
     */
    @Transactional(readOnly = true)
    public String buildMemoryContext(UUID userId, String currentTopicId) {
        try {
            List<StudentMemoryEntry> memories = memoryRepo.findByUserIdAndNotExpired(userId, 10);
            if (memories.isEmpty()) return "";

            List<StudentMemoryEntry> topicMems = memories.stream()
                .filter(m -> currentTopicId != null && currentTopicId.equals(m.getTopicId()))
                .collect(Collectors.toList());

            List<StudentMemoryEntry> otherMems = memories.stream()
                .filter(m -> currentTopicId == null || !currentTopicId.equals(m.getTopicId()))
                .limit(3)
                .collect(Collectors.toList());

            if (topicMems.isEmpty() && otherMems.isEmpty()) return "";

            StringBuilder sb = new StringBuilder("STUDENT MEMORY (past sessions):\n");

            if (!topicMems.isEmpty()) {
                sb.append("About this topic:\n");
                topicMems.forEach(m -> sb.append("  - ").append(m.getSummary()).append("\n"));
            }
            if (!otherMems.isEmpty()) {
                sb.append("Recent context:\n");
                otherMems.forEach(m -> sb.append("  - ").append(m.getSummary()).append("\n"));
            }

            return sb.toString();
        } catch (Exception e) {
            log.warn("[Memory] buildMemoryContext failed userId={}: {}", userId, e.getMessage());
            return "";
        }
    }

    /**
     * Parse conceptTagsDetected from the AI response JSON block.
     * The response format includes ---JSON--- ... ---JSON--- markers.
     */
    public List<String> parseConceptTagsFromResponse(String aiResponse) {
        try {
            if (aiResponse == null) return List.of();
            int jsonStart = aiResponse.indexOf("---JSON---");
            int jsonEnd   = aiResponse.lastIndexOf("---JSON---");
            if (jsonStart == -1 || jsonEnd == jsonStart) return List.of();
            String json = aiResponse.substring(jsonStart + 10, jsonEnd).trim();

            // Simple extraction of conceptTagsDetected array without full parse
            int tagsStart = json.indexOf("\"conceptTagsDetected\"");
            if (tagsStart == -1) return List.of();
            int arrStart = json.indexOf('[', tagsStart);
            int arrEnd   = json.indexOf(']', arrStart);
            if (arrStart == -1 || arrEnd == -1) return List.of();

            String arr = json.substring(arrStart + 1, arrEnd);
            List<String> tags = new ArrayList<>();
            for (String part : arr.split(",")) {
                String cleaned = part.trim().replace("\"", "").strip();
                if (!cleaned.isEmpty()) tags.add(cleaned);
            }
            return tags;
        } catch (Exception e) {
            return List.of();
        }
    }
}
