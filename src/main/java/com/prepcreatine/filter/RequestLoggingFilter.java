package com.prepcreatine.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;

/**
 * RequestLoggingFilter — logs every API request and response time.
 *
 * Output format:
 *   [HTTP →] GET /api/syllabus/review-due
 *   [HTTP ←] GET /api/syllabus/review-due → 200 (12ms) ✓
 *
 * Slow requests (>2s) emit a WARN so they stand out in the logs.
 * Health/actuator endpoints are skipped to reduce noise.
 *
 * Section 14 of the logging specification.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);

    /** Paths that generate too much noise at high frequency — skip verbose logging */
    private static final Set<String> SKIP_PATHS = Set.of(
        "/actuator/health",
        "/actuator/info",
        "/actuator/metrics"
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String path   = request.getRequestURI();
        String method = request.getMethod();

        if (SKIP_PATHS.contains(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        long start = System.currentTimeMillis();

        // Log the incoming request at DEBUG so it doesn't flood INFO in prod
        log.debug("[HTTP →] {} {}", method, path);

        try {
            filterChain.doFilter(request, response);
        } finally {
            long   duration = System.currentTimeMillis() - start;
            int    status   = response.getStatus();

            // Log completed request with timing and HTTP status
            if (status >= 500) {
                log.error("[HTTP ←] {} {} → {} ({}ms) ✗", method, path, status, duration);
            } else if (status >= 400) {
                log.warn("[HTTP ←] {} {} → {} ({}ms)", method, path, status, duration);
            } else {
                log.info("[HTTP ←] {} {} → {} ({}ms) ✓", method, path, status, duration);
            }

            // Warn on slow non-streaming requests
            if (duration > 2000 && !path.contains("/chat/stream")) {
                log.warn("[HTTP] SLOW REQUEST: {} {} took {}ms (target <300ms)", method, path, duration);
            }
        }
    }
}
