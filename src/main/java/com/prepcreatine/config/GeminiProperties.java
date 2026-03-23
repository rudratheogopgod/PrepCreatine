package com.prepcreatine.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Gemini API configuration properties.
 * Bound from application.properties/env vars.
 * Used by GeminiService.
 */
@Configuration
@ConfigurationProperties(prefix = "gemini")
public class GeminiProperties {

    /** Gemini API key. Loaded from GEMINI_API_KEY env var. */
    private String apiKey;

    /** Model to use for chat/generation. Default: llama-3.3-70b-versatile (Groq). */
    private String model = "llama-3.3-70b-versatile";

    /** Max output tokens per request. Default 4096. */
    private int maxOutputTokens = 4096;

    /** Base URL for Groq OpenAI-compatible REST API. */
    private String baseUrl = "https://api.groq.com/openai";

    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }

    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }

    public int getMaxOutputTokens() { return maxOutputTokens; }
    public void setMaxOutputTokens(int maxOutputTokens) { this.maxOutputTokens = maxOutputTokens; }

    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
}
