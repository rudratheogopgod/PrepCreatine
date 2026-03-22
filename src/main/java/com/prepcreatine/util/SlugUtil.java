package com.prepcreatine.util;

/**
 * URL slug generator for conversation titles.
 */
public class SlugUtil {

    private SlugUtil() {}

    /**
     * Converts a title to a URL-safe slug.
     * Example: "Newton's Laws of Motion!" → "newtons-laws-of-motion"
     */
    public static String toSlug(String input) {
        if (input == null || input.isBlank()) return "untitled";
        return input.trim()
                    .toLowerCase()
                    .replaceAll("[^a-z0-9\\s-]", "")
                    .replaceAll("\\s+", "-")
                    .replaceAll("-+", "-")
                    .replaceAll("^-|-$", "");
    }
}
