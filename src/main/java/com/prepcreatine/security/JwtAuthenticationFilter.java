package com.prepcreatine.security;

import com.prepcreatine.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Reads the JWT from the httpOnly cookie named 'prepcreatine_token'.
 * Validates it and sets SecurityContext.
 * [FORBIDDEN] Never throw exceptions on invalid token — just skip setting auth.
 * [FORBIDDEN] Never log the JWT token string itself.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JwtService jwtService;
    private final PrepCreatineUserDetailsService userDetailsService;

    public JwtAuthenticationFilter(JwtService jwtService,
                                   PrepCreatineUserDetailsService userDetailsService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain chain)
            throws ServletException, IOException {

        String token = extractTokenFromCookie(request);

        if (token != null) {
            jwtService.validateAndExtract(token).ifPresent(claims -> {
                try {
                    String userId = jwtService.extractUserId(claims).toString();
                    String email  = jwtService.extractEmail(claims);

                // Only set security context if not already authenticated
                if (SecurityContextHolder.getContext().getAuthentication() == null) {
                    UserDetails userDetails = userDetailsService.loadUserByUsername(email);

                    UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities());
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    SecurityContextHolder.getContext().setAuthentication(authToken);

                    // Add userId to MDC for log correlation
                    MDC.put("userId", userId);
                }
                } catch (Exception e) {
                    // Invalid token state — do not set authentication, let security rules handle 401
                    log.debug("[JwtFilter] Token validation passed but auth setup failed: {}", e.getMessage());
                }
            });
        }

        chain.doFilter(request, response);
    }

    private String extractTokenFromCookie(HttpServletRequest request) {
        if (request.getCookies() == null) return null;
        for (Cookie cookie : request.getCookies()) {
            if ("prepcreatine_token".equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }
}
