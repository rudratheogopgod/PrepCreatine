package com.prepcreatine.util;

import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;

import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * Sanitization utilities per BSDD §6 security requirements.
 * [SECURITY] sanitizeUrl blocks SSRF by rejecting private IP ranges.
 */
public class SanitizationUtil {

    private SanitizationUtil() {}

    /**
     * Strip all HTML tags from user-provided text.
     * Limits length to maxLen characters.
     * Never returns null — empty input returns empty string.
     */
    public static String sanitizeText(String input, int maxLen) {
        if (input == null || input.isBlank()) return "";
        String cleaned = Jsoup.clean(input.trim(), Safelist.none());
        return cleaned.length() > maxLen ? cleaned.substring(0, maxLen) : cleaned;
    }

    /**
     * Sanitize URL for source ingestion.
     * Rejects:
     * - Non http/https schemes (file://, ftp://, etc.)
     * - Private IP ranges (SSRF protection):
     *   10.x.x.x, 172.16-31.x.x, 192.168.x.x, 127.x.x.x, 169.254.x.x, ::1
     *
     * @throws IllegalArgumentException if URL is invalid or blocked
     */
    public static String sanitizeUrl(String url) {
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("URL cannot be blank.");
        }

        URI uri;
        try {
            uri = new URI(url.trim());
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Invalid URL format.");
        }

        String scheme = uri.getScheme();
        if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme)) {
            throw new IllegalArgumentException("Only http and https URLs are allowed.");
        }

        String host = uri.getHost();
        if (host == null) {
            throw new IllegalArgumentException("URL must have a valid host.");
        }

        try {
            InetAddress address = InetAddress.getByName(host);
            if (isPrivateAddress(address)) {
                throw new IllegalArgumentException("Access to private/internal network addresses is not allowed.");
            }
        } catch (java.net.UnknownHostException e) {
            throw new IllegalArgumentException("Unable to resolve host: " + host);
        }

        return uri.toString();
    }

    /**
     * Sanitize mentor code — keep only alphanumeric and hyphens, max 20 chars.
     */
    public static String sanitizeMentorCode(String code) {
        if (code == null) return "";
        return code.trim()
                   .replaceAll("[^a-zA-Z0-9-]", "")
                   .substring(0, Math.min(code.trim().length(), 20))
                   .toUpperCase();
    }

    private static boolean isPrivateAddress(InetAddress address) {
        return address.isLoopbackAddress()
            || address.isLinkLocalAddress()
            || address.isSiteLocalAddress()
            || address.isAnyLocalAddress();
    }
}
