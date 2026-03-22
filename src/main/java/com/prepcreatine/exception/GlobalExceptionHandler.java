package com.prepcreatine.exception;

import com.prepcreatine.util.RequestIdUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

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
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // ── Domain Exceptions ─────────────────────────────────────────────────────

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(
            ResourceNotFoundException ex, HttpServletRequest req) {
        log.warn("[ExHandler] ResourceNotFound: path={}, message={}", req.getServletPath(), ex.getMessage());
        return errorResponse(HttpStatus.NOT_FOUND, ex.getMessage(), req);
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<Map<String, Object>> handleDuplicate(
            DuplicateResourceException ex, HttpServletRequest req) {
        log.warn("[ExHandler] DuplicateResource: path={}, message={}", req.getServletPath(), ex.getMessage());
        return errorResponse(HttpStatus.CONFLICT, ex.getMessage(), req);
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<Map<String, Object>> handleUnauthorized(
            UnauthorizedException ex, HttpServletRequest req) {
        // Do NOT log the specific reason to prevent auth oracle attacks
        log.warn("[ExHandler] Unauthorized: path={}", req.getServletPath());
        return errorResponse(HttpStatus.UNAUTHORIZED, ex.getMessage(), req);
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<Map<String, Object>> handleForbidden(
            ForbiddenException ex, HttpServletRequest req) {
        log.warn("[ExHandler] Forbidden: path={}, userId={}", req.getServletPath(), ex.getUserId());
        return errorResponse(HttpStatus.FORBIDDEN, ex.getMessage(), req);
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(
            ValidationException ex, HttpServletRequest req) {
        log.debug("[ExHandler] Validation: path={}, message={}", req.getServletPath(), ex.getMessage());
        return errorResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), req);
    }

    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<Map<String, Object>> handleRateLimit(
            RateLimitExceededException ex, HttpServletRequest req) {
        log.warn("[ExHandler] RateLimit: path={}", req.getServletPath());
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
            .header("Retry-After", String.valueOf(ex.getRetryAfterSeconds()))
            .body(buildErrorBody(HttpStatus.TOO_MANY_REQUESTS, ex.getMessage(), req));
    }

    @ExceptionHandler(ExternalServiceException.class)
    public ResponseEntity<Map<String, Object>> handleExternalService(
            ExternalServiceException ex, HttpServletRequest req) {
        log.error("[ExHandler] ExternalService failure: service={}, path={}", ex.getServiceName(), req.getServletPath());
        return errorResponse(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage(), req);
    }

    // ── Spring Built-in Exceptions ────────────────────────────────────────────

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleBeanValidation(
            MethodArgumentNotValidException ex, HttpServletRequest req) {
        BindingResult result = ex.getBindingResult();
        String message = result.getFieldErrors().stream()
            .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
            .collect(Collectors.joining(", "));
        log.debug("[ExHandler] BeanValidation: path={}, errors={}", req.getServletPath(), message);
        return errorResponse(HttpStatus.BAD_REQUEST, message, req);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, Object>> handleMaxUpload(
            MaxUploadSizeExceededException ex, HttpServletRequest req) {
        return errorResponse(HttpStatus.BAD_REQUEST, "File exceeds the 20MB limit.", req);
    }

    // ── Catch-all ─────────────────────────────────────────────────────────────

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneric(
            Exception ex, HttpServletRequest req) {
        // Log the full exception internally, but NEVER expose it to the client
        log.error("[ExHandler] Unexpected error: path={}, error={}", req.getServletPath(), ex.getMessage(), ex);
        return errorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
            "An unexpected error occurred. Please try again later.", req);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

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
