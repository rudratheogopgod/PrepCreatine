package com.prepcreatine.exception;

import com.prepcreatine.util.RequestIdUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Global exception handler per BSDD §10.
 * Rules:
 * - Every response includes: timestamp, status, error, message, path, requestId
 * - Stack traces are NEVER included in responses
 * - Generic messages for auth failures (no hint about email/password validity)
 * - [FORBIDDEN] Never expose internal exception details to clients
 * - SSE endpoints get SSE-formatted error events instead of JSON (avoids HttpMessageNotWritableException)
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // ── Domain Exceptions ─────────────────────────────────────────────────────

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(
            ResourceNotFoundException ex, HttpServletRequest req, HttpServletResponse res) throws IOException {
        log.warn("[ExHandler] ResourceNotFound: path={}, message={}", req.getServletPath(), ex.getMessage());
        if (isSseResponse(res)) { writeSseError(res, ex.getMessage()); return null; }
        return errorResponse(HttpStatus.NOT_FOUND, ex.getMessage(), req);
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<Map<String, Object>> handleDuplicate(
            DuplicateResourceException ex, HttpServletRequest req, HttpServletResponse res) throws IOException {
        log.warn("[ExHandler] DuplicateResource: path={}, message={}", req.getServletPath(), ex.getMessage());
        if (isSseResponse(res)) { writeSseError(res, ex.getMessage()); return null; }
        return errorResponse(HttpStatus.CONFLICT, ex.getMessage(), req);
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<Map<String, Object>> handleUnauthorized(
            UnauthorizedException ex, HttpServletRequest req, HttpServletResponse res) throws IOException {
        log.warn("[ExHandler] Unauthorized: path={}", req.getServletPath());
        if (isSseResponse(res)) { writeSseError(res, "Unauthorized"); return null; }
        return errorResponse(HttpStatus.UNAUTHORIZED, ex.getMessage(), req);
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<Map<String, Object>> handleForbidden(
            ForbiddenException ex, HttpServletRequest req, HttpServletResponse res) throws IOException {
        log.warn("[ExHandler] Forbidden: path={}, userId={}", req.getServletPath(), ex.getUserId());
        if (isSseResponse(res)) { writeSseError(res, "Forbidden"); return null; }
        return errorResponse(HttpStatus.FORBIDDEN, ex.getMessage(), req);
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(
            ValidationException ex, HttpServletRequest req, HttpServletResponse res) throws IOException {
        log.debug("[ExHandler] Validation: path={}, message={}", req.getServletPath(), ex.getMessage());
        if (isSseResponse(res)) { writeSseError(res, ex.getMessage()); return null; }
        return errorResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), req);
    }

    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<Map<String, Object>> handleRateLimit(
            RateLimitExceededException ex, HttpServletRequest req, HttpServletResponse res) throws IOException {
        log.warn("[ExHandler] RateLimit: path={}", req.getServletPath());
        if (isSseResponse(res)) { writeSseError(res, "Rate limit exceeded. Please retry."); return null; }
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
            .header("Retry-After", String.valueOf(ex.getRetryAfterSeconds()))
            .body(buildErrorBody(HttpStatus.TOO_MANY_REQUESTS, ex.getMessage(), req));
    }

    @ExceptionHandler(ExternalServiceException.class)
    public ResponseEntity<Map<String, Object>> handleExternalService(
            ExternalServiceException ex, HttpServletRequest req, HttpServletResponse res) throws IOException {
        log.error("[ExHandler] ExternalService failure: service={}, path={}", ex.getServiceName(), req.getServletPath());
        if (isSseResponse(res)) { writeSseError(res, "AI service error. Please retry."); return null; }
        return errorResponse(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage(), req);
    }

    // ── Spring Built-in Exceptions ────────────────────────────────────────────

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleBeanValidation(
            MethodArgumentNotValidException ex, HttpServletRequest req, HttpServletResponse res) throws IOException {
        BindingResult result = ex.getBindingResult();
        String message = result.getFieldErrors().stream()
            .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
            .collect(Collectors.joining(", "));
        log.debug("[ExHandler] BeanValidation: path={}, errors={}", req.getServletPath(), message);
        if (isSseResponse(res)) { writeSseError(res, message); return null; }
        return errorResponse(HttpStatus.BAD_REQUEST, message, req);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, Object>> handleMaxUpload(
            MaxUploadSizeExceededException ex, HttpServletRequest req, HttpServletResponse res) throws IOException {
        if (isSseResponse(res)) { writeSseError(res, "File exceeds the 20MB limit."); return null; }
        return errorResponse(HttpStatus.BAD_REQUEST, "File exceeds the 20MB limit.", req);
    }

    // ── Catch-all ─────────────────────────────────────────────────────────────

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneric(
            Exception ex, HttpServletRequest req, HttpServletResponse res) throws IOException {
        log.error("[ExHandler] Unexpected error: path={}, error={}", req.getServletPath(), ex.getMessage(), ex);
        if (isSseResponse(res)) {
            writeSseError(res, "An unexpected error occurred. Please try again.");
            return null;
        }
        return errorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
            "An unexpected error occurred. Please try again later.", req);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** Returns true if the response is already committed as text/event-stream (SSE). */
    private boolean isSseResponse(HttpServletResponse res) {
        String ct = res.getContentType();
        return ct != null && ct.contains(MediaType.TEXT_EVENT_STREAM_VALUE);
    }

    /**
     * Writes an SSE-formatted error event directly to the already-committed response.
     * This avoids HttpMessageNotWritableException when Spring tries to serialize a Map
     * into a text/event-stream response.
     */
    private void writeSseError(HttpServletResponse res, String message) throws IOException {
        if (!res.isCommitted()) {
            res.setContentType(MediaType.TEXT_EVENT_STREAM_VALUE);
            res.setCharacterEncoding("UTF-8");
        }
        res.getWriter().write("event: error\ndata: " + message + "\n\n");
        res.getWriter().flush();
    }

    private ResponseEntity<Map<String, Object>> errorResponse(
            HttpStatus status, String message, HttpServletRequest req) {
        return ResponseEntity.status(status).body(buildErrorBody(status, message, req));
    }

    private Map<String, Object> buildErrorBody(HttpStatus status, String message, HttpServletRequest req) {
        return Map.of(
            "timestamp", OffsetDateTime.now().toString(),
            "status",    status.value(),
            "error",     status.getReasonPhrase(),
            "message",   message,
            "path",      req.getServletPath(),
            "requestId", RequestIdUtil.currentRequestId()
        );
    }
}

