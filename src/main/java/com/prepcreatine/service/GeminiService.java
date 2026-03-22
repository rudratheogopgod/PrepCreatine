package com.prepcreatine.service;

import com.prepcreatine.config.GeminiProperties;
import com.prepcreatine.exception.ExternalServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

/**
 * Gemini API client per BSDD §6.
 * Uses Spring WebClient (non-blocking).
 *
 * Supports both:
 *   - generateContent  (one-shot JSON response)
 *   - streamGenerateContent (SSE streaming for chat)
 *
 * Models used:
 *   - gemini-2.0-flash  → chat, summaries, Q&A generation (fast)
 *   - text-embedding-004 → embedding (768 dims)
 */
@Service
public class GeminiService {

    private static final Logger log = LoggerFactory.getLogger(GeminiService.class);

    private final WebClient    geminiClient;
    private final GeminiProperties props;

    public GeminiService(@Qualifier("geminiWebClient") WebClient geminiClient, GeminiProperties props) {
        this.geminiClient = geminiClient;
        this.props        = props;
    }

    // ── One-Shot Generation ────────────────────────────────────────────────

    /**
     * Sends a prompt to Gemini and returns the complete text response.
     * Used for: study guide generation, quiz generation, AI summary.
     *
     * @throws ExternalServiceException if Gemini returns an error
     */
    public String generateContent(String systemPrompt, String userPrompt) {
        Map<String, Object> requestBody = buildGenerateRequest(systemPrompt, userPrompt);

        String response = geminiClient.post()
            .uri("/v1beta/models/" + props.getModel() + ":generateContent?key=" + props.getApiKey())
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(requestBody)
            .retrieve()
            .onStatus(status -> !status.is2xxSuccessful(), resp ->
                resp.bodyToMono(String.class)
                    .flatMap(body -> Mono.error(
                        new ExternalServiceException("Gemini",
                            "Gemini API error " + resp.statusCode().value() + ": " + body))))
            .bodyToMono(String.class)
            .block();

        return extractText(response);
    }

    // ── Streaming Generation ───────────────────────────────────────────────

    /**
     * Returns a reactive Flux of SSE text chunks for streaming chat responses.
     * Controller writes each chunk to the SseEmitter.
     */
    public Flux<String> streamGenerateContent(String systemPrompt, String userPrompt) {
        Map<String, Object> requestBody = buildGenerateRequest(systemPrompt, userPrompt);

        return geminiClient.post()
            .uri("/v1beta/models/" + props.getModel() + ":streamGenerateContent?alt=sse&key=" + props.getApiKey())
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(requestBody)
            .retrieve()
            .onStatus(status -> !status.is2xxSuccessful(), resp ->
                Mono.error(new ExternalServiceException("Gemini",
                    "Gemini streaming error " + resp.statusCode().value())))
            .bodyToFlux(String.class)
            .filter(chunk -> chunk != null && !chunk.isBlank())
            .mapNotNull(this::extractStreamChunk);
    }

    // ── Embeddings ─────────────────────────────────────────────────────────

    /**
     * Generates a 768-dimensional embedding vector for a text chunk.
     * Model: text-embedding-004
     */
    public float[] embedText(String text) {
        Map<String, Object> requestBody = Map.of(
            "model", "models/text-embedding-004",
            "content", Map.of(
                "parts", List.of(Map.of("text", text))
            )
        );

        String response = geminiClient.post()
            .uri("/v1beta/models/text-embedding-004:embedContent?key=" + props.getApiKey())
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(requestBody)
            .retrieve()
            .onStatus(status -> !status.is2xxSuccessful(), resp ->
                resp.bodyToMono(String.class)
                    .flatMap(body -> Mono.error(
                        new ExternalServiceException("Gemini", "Embedding error: " + body))))
            .bodyToMono(String.class)
            .block();

        return extractEmbedding(response);
    }

    // ── Private Helpers ────────────────────────────────────────────────────

    private Map<String, Object> buildGenerateRequest(String systemPrompt, String userPrompt) {
        return Map.of(
            "systemInstruction", Map.of(
                "parts", List.of(Map.of("text", systemPrompt))
            ),
            "contents", List.of(Map.of(
                "role", "user",
                "parts", List.of(Map.of("text", userPrompt))
            )),
            "generationConfig", Map.of(
                "temperature",     0.7,
                "maxOutputTokens", props.getMaxOutputTokens()
            )
        );
    }

    /**
     * Parses the full generateContent JSON response to extract text.
     * Uses Jackson via simple string extraction (avoid full deserialize for performance).
     */
    private String extractText(String responseJson) {
        if (responseJson == null) {
            throw new ExternalServiceException("Gemini", "Empty response from Gemini API.");
        }
        try {
            com.fasterxml.jackson.databind.JsonNode root =
                new com.fasterxml.jackson.databind.ObjectMapper().readTree(responseJson);
            return root.path("candidates").get(0)
                       .path("content").path("parts").get(0)
                       .path("text").asText();
        } catch (Exception e) {
            log.error("[Gemini] Failed to parse response: {}", e.getMessage());
            throw new ExternalServiceException("Gemini", "Failed to parse Gemini response.");
        }
    }

    /**
     * Extracts text chunk from a streaming SSE line.
     */
    private String extractStreamChunk(String sseData) {
        try {
            String json = sseData.startsWith("data: ") ? sseData.substring(6) : sseData;
            com.fasterxml.jackson.databind.JsonNode root =
                new com.fasterxml.jackson.databind.ObjectMapper().readTree(json);
            return root.path("candidates").get(0)
                       .path("content").path("parts").get(0)
                       .path("text").asText("");
        } catch (Exception e) {
            return null; // filter out unparseable chunks
        }
    }

    /**
     * Extracts float[] embedding from embedContent response.
     */
    @SuppressWarnings("unchecked")
    private float[] extractEmbedding(String responseJson) {
        try {
            com.fasterxml.jackson.databind.ObjectMapper om = new com.fasterxml.jackson.databind.ObjectMapper();
            com.fasterxml.jackson.databind.JsonNode root = om.readTree(responseJson);
            com.fasterxml.jackson.databind.JsonNode values = root.path("embedding").path("values");
            float[] embedding = new float[values.size()];
            for (int i = 0; i < values.size(); i++) {
                embedding[i] = (float) values.get(i).asDouble();
            }
            return embedding;
        } catch (Exception e) {
            log.error("[Gemini] Failed to parse embedding: {}", e.getMessage());
            throw new ExternalServiceException("Gemini", "Failed to parse embedding response.");
        }
    }
}
