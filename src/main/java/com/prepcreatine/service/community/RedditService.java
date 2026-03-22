package com.prepcreatine.service.community;

import com.prepcreatine.dto.response.RedditPulseResponse;
import com.prepcreatine.dto.response.RedditPostResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Reddit API integration — reads trending posts per exam subreddit.
 *
 * If reddit.client-id or reddit.client-secret is blank, all calls
 * return an empty response (disabled mode) instead of failing.
 * Set REDDIT_ENABLED=true in your .env to activate.
 */
@Service
public class RedditService {

    private static final Logger log = LoggerFactory.getLogger(RedditService.class);

    private final WebClient redditWebClient;

    @Value("${reddit.client-id:}")
    private String clientId;

    @Value("${reddit.client-secret:}")
    private String clientSecret;

    @Value("${reddit.user-agent:PrepCreatine/1.0}")
    private String userAgent;

    @Value("${reddit.token-url:https://www.reddit.com/api/v1/access_token}")
    private String tokenUrl;

    @Value("${reddit.enabled:false}")
    private boolean enabled;

    private static final Map<String, List<String>> EXAM_SUBREDDITS = Map.of(
        "jee",     List.of("r/JEEpreparation", "r/JEENEETprep"),
        "neet",    List.of("r/NEETpreparation", "r/JEENEETprep"),
        "gate-cs", List.of("r/GATE", "r/cscareerquestions"),
        "cat",     List.of("r/CATprep"),
        "upsc",    List.of("r/UPSC", "r/IASpreparation")
    );

    public RedditService(@Qualifier("redditWebClient") WebClient redditWebClient) {
        this.redditWebClient = redditWebClient;
    }

    /**
     * Returns trending Reddit posts for the given exam.
     * Returns empty posts list when Reddit is disabled or credentials are missing.
     */
    @Cacheable(value = "reddit-pulse", key = "#exam")
    public RedditPulseResponse getRedditPulse(String exam) {
        if (!enabled || !StringUtils.hasText(clientId) || !StringUtils.hasText(clientSecret)) {
            log.info("[RedditService] Reddit integration disabled or credentials not configured — returning empty pulse");
            return new RedditPulseResponse(Collections.emptyList(), Instant.now().toString(), true);
        }

        try {
            return fetchPulse(exam);
        } catch (Exception ex) {
            log.warn("[RedditService] Failed to fetch Reddit pulse for exam={}: {}", exam, ex.getMessage());
            return new RedditPulseResponse(Collections.emptyList(), Instant.now().toString(), true);
        }
    }

    private RedditPulseResponse fetchPulse(String exam) {
        // Placeholder implementation — will be expanded when Reddit credentials are provided.
        // Returns empty list for now.
        log.info("[RedditService] Fetching Reddit pulse for exam={}", exam);
        return new RedditPulseResponse(Collections.emptyList(), Instant.now().toString(), false);
    }
}
