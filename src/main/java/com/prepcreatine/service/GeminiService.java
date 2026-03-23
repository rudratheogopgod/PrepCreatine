package com.prepcreatine.service;

import com.prepcreatine.config.GeminiProperties;
import com.prepcreatine.exception.ExternalServiceException;
import com.prepcreatine.util.EvalLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * AI API client using Groq's OpenAI-compatible REST API.
 * Base URL: https://api.groq.com/openai
 *
 * Supports:
 * - generateContent  → POST /v1/chat/completions  (one-shot JSON response)
 * - streamGenerateContent → POST /v1/chat/completions with stream=true (SSE tokens)
 * - embedText        → mock (Groq has no embedding endpoint; returns zero vector)
 *
 * Models:
 * - llama-3.3-70b-versatile → chat, summaries, Q&A generation
 * - llama-3.1-8b-instant    → fallback / fast tasks
 */
@Service
public class GeminiService {

    private static final Logger log = LoggerFactory.getLogger(GeminiService.class);

    private final WebClient geminiClient;
    private final GeminiProperties props;

    public GeminiService(@Qualifier("geminiWebClient") WebClient geminiClient, GeminiProperties props) {
        this.geminiClient = geminiClient;
        this.props = props;
    }

    // ── One-Shot Generation ────────────────────────────────────────────────

    /**
     * Sends a prompt to Groq and returns the complete text response.
     * Uses POST /v1/chat/completions (OpenAI-compatible).
     *
     * @throws ExternalServiceException if Groq returns an error
     */
    public String generateContent(String systemPrompt, String userPrompt) {
        log.info("[Groq] Call start: model={}, promptLength={}chars", props.getModel(), userPrompt.length());
        long start = System.currentTimeMillis();
        Map<String, Object> requestBody = buildChatRequest(systemPrompt, userPrompt, false);

        String response;
        try {
            response = geminiClient.post()
                    .uri("/v1/chat/completions")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + props.getApiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .retrieve()
                    .onStatus(status -> !status.is2xxSuccessful(), resp -> resp.bodyToMono(String.class)
                            .flatMap(body -> {
                                EvalLogger.failure(log, "GROQ",
                                        "API call failed: status=" + resp.statusCode().value() + " body=" + body);
                                return Mono.error(new ExternalServiceException("Groq",
                                        "Groq API error " + resp.statusCode().value() + ": " + body));
                            }))
                    .bodyToMono(String.class)
                    .block();
        } catch (Exception ex) {
            long ms = System.currentTimeMillis() - start;
            EvalLogger.failure(log, "GROQ", "Call failed after " + ms + "ms: " + ex.getMessage());
            throw ex;
        }

        long ms = System.currentTimeMillis() - start;
        String text = extractText(response);
        // Strip markdown code fences if present (Groq sometimes wraps JSON in ```)
        text = stripMarkdownFences(text);
        EvalLogger.result(log, "GROQ", "Response time", ms + "ms");
        EvalLogger.result(log, "GROQ", "Response length", text.length() + " chars");
        if (ms > 10000) {
            EvalLogger.failure(log, "GROQ", "Response took " + ms + "ms — exceeded 10s threshold");
        } else {
            log.debug("[Groq] Call complete: {}ms", ms);
        }
        return text;
    }

    // ── Streaming Generation ───────────────────────────────────────────────

    /**
     * Returns a reactive Flux of SSE text chunks for streaming chat responses.
     * Uses POST /v1/chat/completions with stream=true.
     * Each SSE chunk follows OpenAI delta format: choices[0].delta.content
     */
    public Flux<String> streamGenerateContent(String systemPrompt, String userPrompt) {
        Map<String, Object> requestBody = buildChatRequest(systemPrompt, userPrompt, true);

        return geminiClient.post()
                .uri("/v1/chat/completions")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + props.getApiKey())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .retrieve()
                .onStatus(status -> !status.is2xxSuccessful(), resp -> resp.bodyToMono(String.class)
                        .flatMap(body -> Mono.error(new ExternalServiceException("Groq",
                                "Groq streaming error " + resp.statusCode().value() + ": " + body))))
                .bodyToFlux(String.class)
                .filter(chunk -> chunk != null && !chunk.isBlank())
                .mapNotNull(this::extractStreamChunk);
    }

    // ── Embeddings ─────────────────────────────────────────────────────────

    /**
     * Groq does not provide an embedding endpoint.
     * Returns a zero-filled 768-dimensional vector so callers don't break.
     * If real embeddings are needed, swap in OpenAI text-embedding-3-small.
     */
    public float[] embedText(String text) {
        log.warn("[Embedding] Groq has no embedding API — returning zero vector for text length={}", text.length());
        return new float[768];
    }

    // ── Private Helpers ────────────────────────────────────────────────────

    /**
     * Builds an OpenAI-compatible chat completions request body.
     */
    private Map<String, Object> buildChatRequest(String systemPrompt, String userPrompt, boolean stream) {
        List<Map<String, String>> messages = new ArrayList<>();
        if (systemPrompt != null && !systemPrompt.isBlank()) {
            messages.add(Map.of("role", "system", "content", systemPrompt));
        }
        messages.add(Map.of("role", "user", "content", userPrompt));

        return Map.of(
                "model", props.getModel(),
                "messages", messages,
                "max_tokens", props.getMaxOutputTokens(),
                "temperature", 0.7,
                "stream", stream
        );
    }

    /**
     * Parses a Groq /v1/chat/completions JSON response.
     * Structure: { choices: [ { message: { content: "..." } } ] }
     */
    private String extractText(String responseJson) {
        if (responseJson == null || responseJson.isBlank()) {
            throw new ExternalServiceException("Groq", "Empty response from Groq API.");
        }
        try {
            com.fasterxml.jackson.databind.JsonNode root =
                    new com.fasterxml.jackson.databind.ObjectMapper().readTree(responseJson);

            // Surface Groq API-level errors
            if (root.has("error")) {
                String errMsg = root.path("error").path("message").asText("Unknown Groq error");
                throw new ExternalServiceException("Groq", "Groq API returned error: " + errMsg);
            }

            return root.path("choices").get(0)
                    .path("message")
                    .path("content")
                    .asText();
        } catch (ExternalServiceException ex) {
            throw ex;
        } catch (Exception e) {
            log.error("[Groq] Failed to parse response: {} | raw={}", e.getMessage(), responseJson);
            throw new ExternalServiceException("Groq", "Failed to parse Groq response.");
        }
    }

    /**
     * Extracts a text delta from a streaming SSE chunk.
     * Groq stream format: data: { choices: [ { delta: { content: "..." } } ] }
     * Terminal chunk: data: [DONE]
     */
    private String extractStreamChunk(String sseData) {
        try {
            String json = sseData.startsWith("data: ") ? sseData.substring(6) : sseData;
            if ("[DONE]".equals(json.trim())) return null;

            com.fasterxml.jackson.databind.JsonNode root =
                    new com.fasterxml.jackson.databind.ObjectMapper().readTree(json);
            com.fasterxml.jackson.databind.JsonNode content =
                    root.path("choices").get(0).path("delta").path("content");

            if (content.isMissingNode() || content.isNull()) return null;
            return content.asText("");
        } catch (Exception e) {
            return null; // filter unparseable chunks
        }
    }

    /**
     * Strips markdown code fences (```json ... ```) that Groq sometimes wraps around JSON.
     */
    private String stripMarkdownFences(String text) {
        if (text == null) return null;
        String trimmed = text.trim();
        if (trimmed.startsWith("```")) {
            int firstNewline = trimmed.indexOf('\n');
            if (firstNewline != -1) {
                trimmed = trimmed.substring(firstNewline + 1);
            }
            if (trimmed.endsWith("```")) {
                trimmed = trimmed.substring(0, trimmed.length() - 3).trim();
            }
        }
        return trimmed;
    }
}
