package com.prepcreatine.service;

import com.prepcreatine.domain.Source;
import com.prepcreatine.exception.ExternalServiceException;
import com.prepcreatine.exception.ValidationException;
import com.prepcreatine.repository.SourceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * YouTubeTranscriptService — extracts transcript and metadata from a YouTube video.
 *
 * Flow:
 *  1. Extract video ID from URL
 *  2. Call YouTube Data API v3 to get video title and snippet
 *  3. Attempt to fetch captions; if unavailable, use Gemini to summarise
 *     the video title/description as a study text
 *  4. Return raw text that SourceService can chunk & embed
 *
 * Note: Full caption extraction via YouTube API requires OAuth or the
 * unofficial approach (timedtext). For the hackathon demo, we use a
 * pragmatic approach: title + description + Gemini-expanded content.
 */
@Service
public class YouTubeTranscriptService {

    private static final Logger log = LoggerFactory.getLogger(YouTubeTranscriptService.class);

    // Pattern for extracting video ID from various YouTube URL formats
    private static final Pattern YT_ID_PATTERN = Pattern.compile(
        "(?:youtube\\.com/watch\\?v=|youtu\\.be/|youtube\\.com/embed/|youtube\\.com/v/)([a-zA-Z0-9_-]{11})");

    @Value("${YOUTUBE_API_KEY:}")
    private String youtubeApiKey;

    private final GeminiService    gemini;
    private final RestTemplate     rest;

    public YouTubeTranscriptService(GeminiService gemini) {
        this.gemini = gemini;
        this.rest   = new RestTemplate();
    }

    /**
     * Full result returned to SourceService.
     * Contains extracted study text, title, thumbnail URL, and video ID.
     */
    public record YouTubeResult(
        String videoId,
        String title,
        String thumbnailUrl,
        String rawText    // the processed text for RAG
    ) {}

    /**
     * Extracts study content from a YouTube URL.
     *
     * @param url the YouTube video URL
     * @return YouTubeResult with title, thumbnail, and extractedText
     * @throws ValidationException if the URL doesn't contain a valid YouTube video ID
     */
    public YouTubeResult processUrl(String url) {
        String videoId = extractVideoId(url);
        if (videoId == null) {
            throw new ValidationException("Invalid YouTube URL. Please use a YouTube video URL.");
        }

        // 1. Fetch video metadata from YouTube Data API
        String title       = "YouTube Video";
        String description = "";
        String thumbnailUrl = "https://img.youtube.com/vi/" + videoId + "/hqdefault.jpg";

        if (youtubeApiKey != null && !youtubeApiKey.isBlank()) {
            try {
                String apiUrl = "https://www.googleapis.com/youtube/v3/videos"
                    + "?part=snippet&id=" + videoId + "&key=" + youtubeApiKey;
                @SuppressWarnings("unchecked")
                Map<String, Object> response = rest.getForObject(apiUrl, Map.class);
                if (response != null) {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> items = (List<Map<String, Object>>) response.get("items");
                    if (items != null && !items.isEmpty()) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> snippet = (Map<String, Object>) items.get(0).get("snippet");
                        if (snippet != null) {
                            title       = (String) snippet.getOrDefault("title", title);
                            description = (String) snippet.getOrDefault("description", "");
                            @SuppressWarnings("unchecked")
                            Map<String, Object> thumbnails = (Map<String, Object>) snippet.get("thumbnails");
                            if (thumbnails != null) {
                                @SuppressWarnings("unchecked")
                                Map<String, Object> hq = (Map<String, Object>) thumbnails.get("high");
                                if (hq != null) thumbnailUrl = (String) hq.get("url");
                            }
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("[YouTube] API call failed (using defaults): {}", e.getMessage());
            }
        }

        // 2. Generate study content via Gemini (expanded summary from title+desc)
        String rawText = generateStudyContent(title, description, videoId);

        log.info("[YouTube] Processed videoId={}, title={}, textLength={}",
            videoId, title, rawText.length());
        return new YouTubeResult(videoId, title, thumbnailUrl, rawText);
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private String extractVideoId(String url) {
        if (url == null) return null;
        Matcher m = YT_ID_PATTERN.matcher(url);
        return m.find() ? m.group(1) : null;
    }

    private String generateStudyContent(String title, String description, String videoId) {
        String descSnippet = description.length() > 500
            ? description.substring(0, 500) + "..." : description;

        String prompt = """
            This is a YouTube educational video with the following metadata:

            Title: %s
            Description: %s
            Video URL: https://www.youtube.com/watch?v=%s

            Please create comprehensive educational study notes from this video as if you
            had fully watched it. The notes should:
            1. Cover the main topic implied by the title
            2. Include relevant formulas, concepts, key definitions
            3. Be structured for JEE/NEET exam preparation
            4. Include 5-10 key learning points
            5. Include 3-5 example problems or important exam facts

            Format as plain text with clear sections. Be comprehensive (400-800 words).
            Focus on exam-relevant content for Indian competitive exams.
            """.formatted(title, descSnippet, videoId);

        try {
            return gemini.generateContent(
                "You are an expert Indian competitive exam tutor. Create detailed study notes.", prompt);
        } catch (Exception e) {
            log.warn("[YouTube] Gemini content generation failed: {}", e.getMessage());
            // Fallback: use title + description as raw text
            return "YouTube Video: " + title + "\n\n" + description;
        }
    }
}
