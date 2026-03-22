package com.prepcreatine.dto.response;

import java.util.List;

/**
 * Response for the GET /api/community/reddit-pulse endpoint.
 */
public record RedditPulseResponse(
    List<RedditPostResponse> posts,
    String fetchedAt,
    boolean disabled
) {}
