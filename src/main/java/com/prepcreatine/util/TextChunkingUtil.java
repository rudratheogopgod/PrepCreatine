package com.prepcreatine.util;

import java.util.ArrayList;
import java.util.List;

/**
 * Splits long text into overlapping chunks for embedding and RAG.
 * Per BSDD §6: ~500 token chunks with 50-token overlap.
 * Approximation: 1 token ≈ 4 characters.
 * Splits at sentence boundaries where possible to preserve context.
 */
public class TextChunkingUtil {

    private static final int DEFAULT_CHUNK_SIZE = 500;   // tokens (~2000 chars)
    private static final int DEFAULT_OVERLAP    = 50;    // tokens (~200 chars)
    private static final int CHARS_PER_TOKEN    = 4;

    private TextChunkingUtil() {}

    public static List<String> chunk(String text) {
        return chunk(text, DEFAULT_CHUNK_SIZE, DEFAULT_OVERLAP);
    }

    public static List<String> chunk(String text, int chunkSizeTokens, int overlapTokens) {
        if (text == null || text.isBlank()) return List.of();

        int chunkSizeChars = chunkSizeTokens * CHARS_PER_TOKEN;    // ~2000
        int overlapChars   = overlapTokens   * CHARS_PER_TOKEN;    // ~200

        // Split into sentences first (split on . ! ? followed by space or newline)
        String[] sentences = text.split("(?<=[.!?])\\s+");

        List<String> chunks = new ArrayList<>();
        StringBuilder currentChunk = new StringBuilder();

        for (String sentence : sentences) {
            // If adding this sentence would exceed chunk size, save current chunk
            if (currentChunk.length() > 0
                && currentChunk.length() + sentence.length() + 1 > chunkSizeChars) {

                String chunk = currentChunk.toString().trim();
                if (!chunk.isEmpty()) chunks.add(chunk);

                // Start next chunk with overlap from end of current chunk
                String currentStr = currentChunk.toString();
                int overlapStart  = Math.max(0, currentStr.length() - overlapChars);
                currentChunk = new StringBuilder(currentStr.substring(overlapStart));
            }

            if (currentChunk.length() > 0) currentChunk.append(' ');
            currentChunk.append(sentence);
        }

        // Add last chunk
        String remaining = currentChunk.toString().trim();
        if (!remaining.isEmpty()) chunks.add(remaining);

        return chunks;
    }
}
