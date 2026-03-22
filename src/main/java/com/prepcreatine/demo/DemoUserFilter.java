package com.prepcreatine.demo;

import com.prepcreatine.config.DemoModeConfig;
import com.prepcreatine.domain.User;
import com.prepcreatine.repository.UserRepository;
import com.prepcreatine.security.PrepCreatineUserDetails;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Demo mode filter — injects demo user (Arjun Sharma) into every request's SecurityContext.
 * BSDD v2.1 §5: DemoUserFilter.java [NEW]
 *
 * Only active when app.demo-mode=true. Completely absent in production context.
 * [FORBIDDEN] Never suppress exceptions here — fail loudly if demo user cannot be loaded.
 */
@Component
@ConditionalOnProperty(name = "app.demo-mode", havingValue = "true")
public class DemoUserFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(DemoUserFilter.class);

    private final UserRepository userRepository;

    public DemoUserFilter(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        log.debug("[DemoUserFilter] Request: {} {} → injecting demo user Arjun Sharma",
                  request.getMethod(), request.getRequestURI());

        // [REQUIRED] Load demo user from DB every request — never cache cross-request
        User demoUser = userRepository.findById(DemoModeConfig.DEMO_USER_ID)
                .orElseThrow(() -> new IllegalStateException(
                        "[DemoUserFilter] Demo user not found. " +
                        "Check DemoUserSeeder ran successfully."));

        PrepCreatineUserDetails userDetails = new PrepCreatineUserDetails(demoUser);

        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());

        auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

        // Set demo user as the authenticated principal for this request
        SecurityContextHolder.getContext().setAuthentication(auth);

        // Add userId to MDC for structured logging
        MDC.put("userId", DemoModeConfig.DEMO_USER_ID.toString());
        MDC.put("demoMode", "true");

        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove("userId");
            MDC.remove("demoMode");
            SecurityContextHolder.clearContext();
        }
    }
}
