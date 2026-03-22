package com.prepcreatine.util;

import org.slf4j.MDC;

/**
 * Reads requestId from MDC for inclusion in error responses.
 */
public class RequestIdUtil {

    private RequestIdUtil() {}

    public static String currentRequestId() {
        String id = MDC.get("requestId");
        return id != null ? id : "unknown";
    }
}
