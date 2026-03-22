package com.prepcreatine.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * JWT configuration properties bound from application.properties/env vars.
 * Used by JwtService.
 */
@Configuration
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    /** HMAC-SHA256 secret — must be >= 256 bits. Loaded from JWT_SECRET env var. */
    private String secret;

    /** Access token TTL. Default 15 minutes. Loaded from JWT_ACCESS_TTL. */
    private Duration accessTtl = Duration.ofMinutes(15);

    /** Refresh token TTL. Default 7 days. Loaded from JWT_REFRESH_TTL. */
    private Duration refreshTtl = Duration.ofDays(7);

    public String getSecret() { return secret; }
    public void setSecret(String secret) { this.secret = secret; }

    public Duration getAccessTtl() { return accessTtl; }
    public void setAccessTtl(Duration accessTtl) { this.accessTtl = accessTtl; }

    public Duration getRefreshTtl() { return refreshTtl; }
    public void setRefreshTtl(Duration refreshTtl) { this.refreshTtl = refreshTtl; }
}
