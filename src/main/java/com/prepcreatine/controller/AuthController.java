package com.prepcreatine.controller;

import com.prepcreatine.dto.request.*;
import com.prepcreatine.dto.response.UserResponse;
import com.prepcreatine.service.AuthService;
import com.prepcreatine.service.UserService;
import com.prepcreatine.util.SecurityUtil;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * Authentication endpoints per BSDD §8.
 *
 * POST /api/auth/signup
 * POST /api/auth/login
 * POST /api/auth/refresh
 * POST /api/auth/verify-email
 * POST /api/auth/resend-verification
 * POST /api/auth/forgot-password
 * POST /api/auth/reset-password
 * PUT  /api/me/password
 */
@RestController
public class AuthController {

    private final AuthService authService;
    private final UserService userService;

    public AuthController(AuthService authService, UserService userService) {
        this.authService = authService;
        this.userService = userService;
    }

    @PostMapping("/api/auth/signup")
    public ResponseEntity<Map<String, Object>> signup(@Valid @RequestBody SignupRequest req) {
        Map<String, Object> result = authService.signup(req);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @PostMapping("/api/auth/login")
    public ResponseEntity<Map<String, Object>> login(@Valid @RequestBody LoginRequest req) {
        return ResponseEntity.ok(authService.login(req));
    }

    @PostMapping("/api/auth/refresh")
    public ResponseEntity<Map<String, Object>> refresh(@RequestBody Map<String, String> body) {
        String refreshToken = body.get("refreshToken");
        if (refreshToken == null || refreshToken.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "refreshToken is required."));
        }
        return ResponseEntity.ok(authService.refresh(refreshToken));
    }

    @PostMapping("/api/auth/verify-email")
    public ResponseEntity<Map<String, String>> verifyEmail(@Valid @RequestBody VerifyEmailRequest req) {
        authService.verifyEmail(req);
        return ResponseEntity.ok(Map.of("message", "Email verified successfully."));
    }

    @PostMapping("/api/auth/resend-verification")
    public ResponseEntity<Map<String, String>> resendVerification() {
        UUID userId = SecurityUtil.getCurrentUserId();
        authService.resendVerificationEmail(userId);
        return ResponseEntity.ok(Map.of("message", "Verification email sent."));
    }

    @PostMapping("/api/auth/forgot-password")
    public ResponseEntity<Map<String, String>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest req) {
        authService.forgotPassword(req);
        // [SECURITY] Always return OK regardless of whether email exists
        return ResponseEntity.ok(Map.of("message", "If that email exists, a reset link has been sent."));
    }

    @PostMapping("/api/auth/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(@Valid @RequestBody ResetPasswordRequest req) {
        authService.resetPassword(req);
        return ResponseEntity.ok(Map.of("message", "Password reset successfully."));
    }

    @PutMapping("/api/me/password")
    public ResponseEntity<Map<String, String>> changePassword(@Valid @RequestBody ChangePasswordRequest req) {
        authService.changePassword(req, SecurityUtil.getCurrentUserId());
        return ResponseEntity.ok(Map.of("message", "Password changed successfully."));
    }

    /** POST /api/auth/logout — clears the security context; JWT is stateless so
     *  the client simply discards the token after this call. */
    @PostMapping("/api/auth/logout")
    public ResponseEntity<Map<String, String>> logout() {
        SecurityContextHolder.clearContext();
        return ResponseEntity.ok(Map.of("message", "Logged out successfully."));
    }

    /** GET /api/auth/me — returns the currently authenticated user. */
    @GetMapping("/api/auth/me")
    public ResponseEntity<UserResponse> getMe() {
        return ResponseEntity.ok(userService.getMe(SecurityUtil.getCurrentUserId()));
    }
}
