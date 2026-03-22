package com.prepcreatine.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * General application properties (email, frontend URL, etc).
 * Bound from application.properties/env vars.
 * Used by EmailService, OnboardingService, etc.
 */
@Configuration
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    /** Sender email address. Loaded from APP_EMAIL_FROM env var. */
    private String emailFrom = "noreply@prepcreatine.com";

    /** Frontend base URL. Loaded from APP_FRONTEND_URL env var. */
    private String frontendUrl = "http://localhost:3000";

    /** Max import text characters. */
    private int maxTextImportChars = 500_000;

    /** Max PDF size in bytes (default 20MB). */
    private long maxPdfBytes = 20 * 1024 * 1024;

    public String getEmailFrom() { return emailFrom; }
    public void setEmailFrom(String emailFrom) { this.emailFrom = emailFrom; }

    public String getFrontendUrl() { return frontendUrl; }
    public void setFrontendUrl(String frontendUrl) { this.frontendUrl = frontendUrl; }

    public int getMaxTextImportChars() { return maxTextImportChars; }
    public void setMaxTextImportChars(int maxTextImportChars) { this.maxTextImportChars = maxTextImportChars; }

    public long getMaxPdfBytes() { return maxPdfBytes; }
    public void setMaxPdfBytes(long maxPdfBytes) { this.maxPdfBytes = maxPdfBytes; }
}
