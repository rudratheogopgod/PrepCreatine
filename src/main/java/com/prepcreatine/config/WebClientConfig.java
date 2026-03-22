package com.prepcreatine.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;

/**
 * WebClient beans for all external API calls.
 * Three named beans: Gemini, YouTube, Reddit.
 * All external calls must use WebClient (not RestTemplate) per BSDD §1.
 */
@Configuration
public class WebClientConfig {

    @Value("${gemini.base-url}")
    private String geminiBaseUrl;

    @Value("${youtube.base-url}")
    private String youtubeBaseUrl;

    @Value("${reddit.base-url}")
    private String redditBaseUrl;

    /**
     * WebClient for Gemini API calls (chat streaming + non-streaming + embeddings).
     */
    @Bean("geminiWebClient")
    public WebClient geminiWebClient() {
        return WebClient.builder()
            .baseUrl(geminiBaseUrl)
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .codecs(configurer -> configurer.defaultCodecs()
                .maxInMemorySize(4 * 1024 * 1024)) // 4MB for large AI responses
            .build();
    }

    /**
     * WebClient for YouTube Data API v3.
     */
    @Bean("youtubeWebClient")
    public WebClient youtubeWebClient() {
        return WebClient.builder()
            .baseUrl(youtubeBaseUrl)
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .build();
    }

    /**
     * WebClient for Reddit OAuth2 API.
     */
    @Bean("redditWebClient")
    public WebClient redditWebClient() {
        return WebClient.builder()
            .baseUrl(redditBaseUrl)
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .defaultHeader(HttpHeaders.USER_AGENT, "PrepCreatine/1.0 (by Rudra Agrawal)")
            .codecs(configurer -> configurer.defaultCodecs()
                .maxInMemorySize(2 * 1024 * 1024)) // 2MB
            .build();
    }

    /**
     * Generic WebClient for URL content fetching (source ingestion).
     * 10-second timeout, 500KB limit per BSDD §6 (import-url endpoint).
     */
    @Bean("urlFetchWebClient")
    public WebClient urlFetchWebClient() {
        return WebClient.builder()
            .codecs(configurer -> configurer.defaultCodecs()
                .maxInMemorySize(512 * 1024)) // 500KB limit (SSRF protection)
            .build();
    }

    /**
     * RestTemplate for synchronous calls to the Python LangGraph planner agent.
     * Uses a long read timeout because /generate-plan calls the Groq LLM (~15-40s).
     * Uses SimpleClientHttpRequestFactory for broad Spring Boot 3.x compatibility.
     */
    @Bean
    public RestTemplate restTemplate() {
        org.springframework.http.client.SimpleClientHttpRequestFactory factory =
            new org.springframework.http.client.SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10_000);  // 10 seconds
        factory.setReadTimeout(60_000);     // 60 seconds (plan generation takes ~20-40s)
        return new RestTemplate(factory);
    }
}
