package com.prepcreatine.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.caffeine.CaffeineProxyManager;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Bucket4j rate limiting configuration per BSDD §5.
 *
 * Limits per endpoint:
 *   POST /api/auth/login:          5 requests per 15 minutes (brute force protection)
 *   POST /api/auth/signup:         3 requests per 10 minutes
 *   POST /api/auth/forgot-password: 3 requests per hour
 *   POST /api/chat/stream:         20 requests per minute per authenticated user
 *   POST /api/sources/import-url:  10 requests per minute per user
 *   All other:                     120 requests per minute per IP
 */
@Configuration
public class RateLimitConfig {

    /**
     * Caffeine-backed Bucket4j ProxyManager.
     * Key = IP address string (or userId for per-user limits).
     */
    @Bean
    public ProxyManager<String> rateLimitProxyManager() {
        // IMPORTANT: do NOT call expireAfterAccess() or expireAfterWrite()
        // on this builder. CaffeineProxyManager sets its own variable Expiry policy
        // via expireAfter() internally, and Caffeine throws if both are set.
        Caffeine<Object, Object> builder = Caffeine.newBuilder()
            .maximumSize(100_000);
        return new CaffeineProxyManager<>(builder, Duration.ofHours(1));
    }

    // ── Bucket configurations (shared; applied per key in RateLimitFilter) ────────

    public static BucketConfiguration loginBucketConfig() {
        return BucketConfiguration.builder()
            .addLimit(limit -> limit.capacity(5).refillGreedy(5, Duration.ofMinutes(15)))
            .build();
    }

    public static BucketConfiguration signupBucketConfig() {
        return BucketConfiguration.builder()
            .addLimit(limit -> limit.capacity(3).refillGreedy(3, Duration.ofMinutes(10)))
            .build();
    }

    public static BucketConfiguration forgotPasswordBucketConfig() {
        return BucketConfiguration.builder()
            .addLimit(limit -> limit.capacity(3).refillGreedy(3, Duration.ofHours(1)))
            .build();
    }

    public static BucketConfiguration chatStreamBucketConfig() {
        return BucketConfiguration.builder()
            .addLimit(limit -> limit.capacity(20).refillGreedy(20, Duration.ofMinutes(1)))
            .build();
    }

    public static BucketConfiguration importUrlBucketConfig() {
        return BucketConfiguration.builder()
            .addLimit(limit -> limit.capacity(10).refillGreedy(10, Duration.ofMinutes(1)))
            .build();
    }

    public static BucketConfiguration defaultBucketConfig() {
        return BucketConfiguration.builder()
            .addLimit(limit -> limit.capacity(120).refillGreedy(120, Duration.ofMinutes(1)))
            .build();
    }
}
