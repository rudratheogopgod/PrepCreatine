package com.prepcreatine.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.prepcreatine.config.RateLimitConfig;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Bucket4j rate limiting filter per BSDD §5.
 * Applied per IP (X-Forwarded-For aware) for all endpoints.
 * Per-user limits applied at service layer (injected into bucket key).
 * Returns 429 + Retry-After header when limit exceeded.
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);

    private final ProxyManager<String> proxyManager;
    private final ObjectMapper objectMapper;

    public RateLimitFilter(ProxyManager<String> proxyManager, ObjectMapper objectMapper) {
        this.proxyManager = proxyManager;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain chain)
            throws ServletException, IOException {

        String ip        = resolveClientIp(request);
        String path      = request.getServletPath();
        String method    = request.getMethod();

        String bucketKey = method + ":" + path + ":" + ip;
        Supplier<BucketConfiguration> configSupplier = resolveConfig(path, method);

        Bucket bucket = proxyManager.builder().build(bucketKey, configSupplier);
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

        if (probe.isConsumed()) {
            chain.doFilter(request, response);
        } else {
            long retryAfterSeconds = probe.getNanosToWaitForRefill() / 1_000_000_000L;
            log.warn("[RateLimit] limit exceeded: ip={}, path={}, retryAfter={}s", ip, path, retryAfterSeconds);

            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setHeader("Retry-After", String.valueOf(retryAfterSeconds));

            Map<String, Object> errorBody = Map.of(
                "timestamp", OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
                "status", 429,
                "error", "Too Many Requests",
                "message", "Rate limit exceeded. Please retry after " + retryAfterSeconds + " seconds.",
                "path", path
            );
            objectMapper.writeValue(response.getWriter(), errorBody);
        }
    }

    private Supplier<BucketConfiguration> resolveConfig(String path, String method) {
        if ("POST".equals(method) && path.equals("/api/auth/login"))
            return RateLimitConfig::loginBucketConfig;
        if ("POST".equals(method) && path.equals("/api/auth/signup"))
            return RateLimitConfig::signupBucketConfig;
        if ("POST".equals(method) && path.equals("/api/auth/forgot-password"))
            return RateLimitConfig::forgotPasswordBucketConfig;
        if ("POST".equals(method) && path.equals("/api/chat/stream"))
            return RateLimitConfig::chatStreamBucketConfig;
        if ("POST".equals(method) && path.equals("/api/sources/import-url"))
            return RateLimitConfig::importUrlBucketConfig;
        return RateLimitConfig::defaultBucketConfig;
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
