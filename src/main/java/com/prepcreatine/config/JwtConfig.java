package com.prepcreatine.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * JWT configuration properties.
 * All values loaded from environment variables via application.properties bindings.
 */
@Configuration
@ConfigurationProperties(prefix = "jwt")
public class JwtConfig {

    private String secret;
    private int expiryDays = 7;
    private String cookieName = "prepcreatine_token";
    private String cookieDomain = "localhost";
    private boolean cookieSecure = true;

    // ── Getters & Setters ─────────────────────────────────────────────────────────

    public String getSecret() { return secret; }
    public void setSecret(String secret) { this.secret = secret; }

    public int getExpiryDays() { return expiryDays; }
    public void setExpiryDays(int expiryDays) { this.expiryDays = expiryDays; }

    public String getCookieName() { return cookieName; }
    public void setCookieName(String cookieName) { this.cookieName = cookieName; }

    public String getCookieDomain() { return cookieDomain; }
    public void setCookieDomain(String cookieDomain) { this.cookieDomain = cookieDomain; }

    public boolean isCookieSecure() { return cookieSecure; }
    public void setCookieSecure(boolean cookieSecure) { this.cookieSecure = cookieSecure; }

    /**
     * Expiry in seconds for cookie maxAge and JWT exp claim.
     */
    public long getExpirySeconds() {
        return (long) expiryDays * 24 * 60 * 60;
    }
}
