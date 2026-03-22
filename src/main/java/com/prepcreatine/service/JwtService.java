package com.prepcreatine.service;

import com.prepcreatine.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * JWT token generation and validation.
 * Per BSDD §8:
 * - Algorithm  : HS256 (symmetric)
 * - Access TTL : 15 min (configurable)
 * - Refresh TTL: 7 days (configurable)
 * - Claims     : sub (userId), role, type (access|refresh)
 */
@Service
public class JwtService {

    private static final Logger log = LoggerFactory.getLogger(JwtService.class);

    private final JwtProperties jwtProperties;
    private final SecretKey     signingKey;

    public JwtService(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
        byte[] keyBytes = jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8);
        this.signingKey  = Keys.hmacShaKeyFor(keyBytes);
    }

    // ── Token Generation ──────────────────────────────────────────────────────

    public String generateAccessToken(UUID userId, String role, String email) {
        return Jwts.builder()
            .subject(userId.toString())
            .claim("role", role)
            .claim("email", email)
            .claim("type", "access")
            .issuedAt(new Date())
            .expiration(new Date(System.currentTimeMillis() + jwtProperties.getAccessTtl().toMillis()))
            .signWith(signingKey)
            .compact();
    }

    public String generateRefreshToken(UUID userId) {
        return Jwts.builder()
            .subject(userId.toString())
            .claim("type", "refresh")
            .id(UUID.randomUUID().toString())   // jti — for future revocation list
            .issuedAt(new Date())
            .expiration(new Date(System.currentTimeMillis() + jwtProperties.getRefreshTtl().toMillis()))
            .signWith(signingKey)
            .compact();
    }

    // ── Token Validation ──────────────────────────────────────────────────────

    /**
     * Validates the token and returns its claims.
     * Returns Optional.empty() if token is expired, malformed, or tampered.
     * [SECURITY] Logs only at WARN level; stack trace swallowed by design.
     */
    public Optional<Claims> validateAndExtract(String token) {
        try {
            Claims claims = Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
            return Optional.of(claims);
        } catch (SignatureException e) {
            log.warn("[JWT] Invalid signature");
        } catch (io.jsonwebtoken.ExpiredJwtException e) {
            log.debug("[JWT] Token expired");
        } catch (Exception e) {
            log.warn("[JWT] Malformed token: {}", e.getClass().getSimpleName());
        }
        return Optional.empty();
    }

    public UUID extractUserId(Claims claims) {
        return UUID.fromString(claims.getSubject());
    }

    public String extractRole(Claims claims) {
        return claims.get("role", String.class);
    }

    public String extractType(Claims claims) {
        return claims.get("type", String.class);
    }

    public String extractEmail(Claims claims) {
        return claims.get("email", String.class);
    }
}
