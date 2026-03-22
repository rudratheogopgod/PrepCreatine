package com.prepcreatine.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

/**
 * Caffeine in-memory cache configuration — 8 cache regions per BSDD §12.
 *
 * Cache regions:
 *   youtube-results     24h  1000 entries
 *   reddit-pulse        30m   200 entries
 *   syllabus-tree       never  20 entries (evict on restart)
 *   exam-config         never  20 entries
 *   roadmap-priorities   1h  5000 entries
 *   analytics            5m  5000 entries
 *   quiz-explanations   24h 10000 entries
 *   embeddings           1h  5000 entries
 */
@Configuration
public class CacheConfig {

    public static final String YOUTUBE_RESULTS      = "youtube-results";
    public static final String REDDIT_PULSE         = "reddit-pulse";
    public static final String SYLLABUS_TREE        = "syllabus-tree";
    public static final String EXAM_CONFIG          = "exam-config";
    public static final String ROADMAP_PRIORITIES   = "roadmap-priorities";
    public static final String ANALYTICS            = "analytics";
    public static final String QUIZ_EXPLANATIONS    = "quiz-explanations";
    public static final String EMBEDDINGS           = "embeddings";

    @Bean
    @Primary
    public CacheManager cacheManager() {
        // Use a SimpleCacheManager with per-cache Caffeine instances
        org.springframework.cache.support.SimpleCacheManager simpleCacheManager =
                new org.springframework.cache.support.SimpleCacheManager();

        simpleCacheManager.setCaches(Arrays.asList(
            buildCache(YOUTUBE_RESULTS,    1000, 24, TimeUnit.HOURS),
            buildCache(REDDIT_PULSE,        200, 30, TimeUnit.MINUTES),
            buildCacheNoExpiry(SYLLABUS_TREE, 20),
            buildCacheNoExpiry(EXAM_CONFIG,   20),
            buildCache(ROADMAP_PRIORITIES, 5000,  1, TimeUnit.HOURS),
            buildCache(ANALYTICS,          5000,  5, TimeUnit.MINUTES),
            buildCache(QUIZ_EXPLANATIONS, 10000, 24, TimeUnit.HOURS),
            buildCache(EMBEDDINGS,         5000,  1, TimeUnit.HOURS)
        ));

        return simpleCacheManager;
    }

    private org.springframework.cache.caffeine.CaffeineCache buildCache(
            String name, int maxSize, long duration, TimeUnit unit) {
        return new org.springframework.cache.caffeine.CaffeineCache(
            name,
            Caffeine.newBuilder()
                .maximumSize(maxSize)
                .expireAfterWrite(duration, unit)
                .recordStats()
                .build()
        );
    }

    private org.springframework.cache.caffeine.CaffeineCache buildCacheNoExpiry(
            String name, int maxSize) {
        return new org.springframework.cache.caffeine.CaffeineCache(
            name,
            Caffeine.newBuilder()
                .maximumSize(maxSize)
                .recordStats()
                .build()
        );
    }
}
