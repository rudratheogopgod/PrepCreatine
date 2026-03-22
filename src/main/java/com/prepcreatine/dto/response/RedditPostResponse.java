package com.prepcreatine.dto.response;

/**
 * Single Reddit post within a RedditPulseResponse.
 */
public record RedditPostResponse(
    String title,
    String url,
    int upvotes,
    String subreddit,
    String aiSummary
) {}
