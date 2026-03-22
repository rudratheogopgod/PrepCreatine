package com.prepcreatine.domain;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * EmailVerificationToken — maps to `email_verification_tokens` table.
 * Shared for both email verification and password reset flows.
 * type: 'email_verify' | 'password_reset'
 * used_at: null until token is consumed — prevents replay attacks.
 */
@Entity
@Table(name = "email_verification_tokens")
public class EmailVerificationToken {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "token", nullable = false, unique = true, length = 128)
    private String token;

    @Column(name = "type", nullable = false, length = 20)
    private String type; // 'email_verify' or 'password_reset'

    @Column(name = "expires_at", nullable = false)
    private OffsetDateTime expiresAt;

    @Column(name = "used_at")
    private OffsetDateTime usedAt; // null = not used yet

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    public EmailVerificationToken() {}

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public OffsetDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(OffsetDateTime expiresAt) { this.expiresAt = expiresAt; }
    public OffsetDateTime getUsedAt() { return usedAt; }
    public void setUsedAt(OffsetDateTime usedAt) { this.usedAt = usedAt; }
    public OffsetDateTime getCreatedAt() { return createdAt; }

    public boolean isExpired() { return OffsetDateTime.now().isAfter(expiresAt); }
    public boolean isUsed() { return usedAt != null; }
    public boolean isValid() { return !isExpired() && !isUsed(); }
}
