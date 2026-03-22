package com.prepcreatine.util;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

/**
 * Helper to extract the current authenticated user's ID from the
 * Spring Security context. The UUID is set as the principal by
 * JwtAuthenticationFilter.
 */
public final class SecurityUtil {

    private SecurityUtil() {}

    /**
     * Returns the authenticated user's UUID.
     * Throws IllegalStateException if not authenticated (should not happen
     * for endpoints behind Spring Security).
     */
    public static UUID getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getPrincipal() == null) {
            throw new IllegalStateException("No authenticated user in security context.");
        }
        Object principal = auth.getPrincipal();
        if (principal instanceof UUID) {
            return (UUID) principal;
        }
        // Fallback: principal stored as String UUID
        return UUID.fromString(principal.toString());
    }

    /**
     * Returns the current authenticated user's UUID, or null if unauthenticated.
     * Used by public endpoints that show different data for logged-in users
     * (e.g. upvotedByMe on community threads).
     */
    public static UUID getCurrentUserIdOptional() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated()) return null;
            Object principal = auth.getPrincipal();
            if (principal instanceof UUID) return (UUID) principal;
            if ("anonymousUser".equals(principal)) return null;
            return UUID.fromString(principal.toString());
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Returns the current user's role (e.g. "STUDENT", "MENTOR", "ADMIN").
     * Based on the single GrantedAuthority set by JwtAuthenticationFilter.
     */
    public static String getCurrentUserRole() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getAuthorities() == null) return null;
        return auth.getAuthorities().stream()
            .findFirst()
            .map(a -> a.getAuthority().replace("ROLE_", ""))
            .orElse(null);
    }
}
