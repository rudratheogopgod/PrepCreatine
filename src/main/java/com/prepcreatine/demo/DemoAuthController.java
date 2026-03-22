package com.prepcreatine.demo;

import com.prepcreatine.config.DemoModeConfig;
import com.prepcreatine.dto.response.UserResponse;
import com.prepcreatine.repository.UserRepository;
import com.prepcreatine.service.UserMapper;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Demo mode auth controller — overrides login/signup/me for demo mode.
 * BSDD v2.1 §5: DemoAuthController.java [NEW]
 *
 * This controller takes priority over AuthController in demo mode by mapping
 * the same /api/auth/** paths while AuthController's @ConditionalOnMissingBean
 * or ordering ensures no conflict. In production (DEMO_MODE=false), this class
 * does not exist in the Spring context. @ConditionalOnProperty excludes it.
 *
 * [NOTE] In production (DEMO_MODE=false), this class does not exist in the
 * Spring context. The real AuthController handles all /api/auth/** requests.
 */
@RestController
@RequestMapping("/api/auth")
@ConditionalOnProperty(name = "app.demo-mode", havingValue = "true")
public class DemoAuthController {

    private static final Logger log = LoggerFactory.getLogger(DemoAuthController.class);

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    public DemoAuthController(UserRepository userRepository, UserMapper userMapper) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
    }

    /**
     * POST /api/auth/login — ignores credentials, returns demo user.
     * Sets a fake cookie so frontend cookie-presence checks don't break.
     */
    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody(required = false) Map<String, Object> req,
                                                     HttpServletResponse response) {
        log.info("[DemoAuth] login called → returning demo user");
        addDemoCookie(response);
        return ResponseEntity.ok(buildDemoLoginResponse());
    }

    /**
     * POST /api/auth/signup — ignores all fields, returns demo user as 201.
     */
    @PostMapping("/signup")
    public ResponseEntity<Map<String, Object>> signup(@RequestBody(required = false) Map<String, Object> req,
                                                      HttpServletResponse response) {
        log.info("[DemoAuth] signup called → returning demo user");
        addDemoCookie(response);
        return ResponseEntity.status(201).body(buildDemoLoginResponse());
    }

    /**
     * GET /api/auth/me — always returns Arjun Sharma's UserResponse.
     */
    @GetMapping("/me")
    public ResponseEntity<UserResponse> me() {
        log.info("[DemoAuth] /me called → returning demo user");
        UserResponse userResponse = userRepository.findById(DemoModeConfig.DEMO_USER_ID)
                .map(userMapper::toUserResponse)
                .orElseThrow(() -> new IllegalStateException(
                        "[DemoAuthController] Demo user not found. Check DemoUserSeeder."));
        return ResponseEntity.ok(userResponse);
    }

    /**
     * POST /api/auth/logout — clears cookie, returns 204.
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletResponse response) {
        log.info("[DemoAuth] logout called → clearing cookie");
        Cookie cookie = new Cookie("prepcreatine_token", "");
        cookie.setMaxAge(0);
        cookie.setPath("/");
        response.addCookie(cookie);
        return ResponseEntity.noContent().build();
    }

    /**
     * POST /api/auth/forgot-password — always returns the generic OK message.
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, String>> forgotPassword(@RequestBody(required = false) Map<String, Object> req) {
        log.info("[DemoAuth] forgot-password called → returning demo message");
        return ResponseEntity.ok(Map.of("message",
                "If that email has an account, we've sent a reset link. Check your inbox."));
    }

    /**
     * POST /api/auth/verify-email — returns demo user as if verified.
     */
    @PostMapping("/verify-email")
    public ResponseEntity<UserResponse> verifyEmail(@RequestBody(required = false) Map<String, Object> req) {
        log.info("[DemoAuth] verify-email called → returning demo user");
        UserResponse userResponse = userRepository.findById(DemoModeConfig.DEMO_USER_ID)
                .map(userMapper::toUserResponse)
                .orElseThrow(() -> new IllegalStateException(
                        "[DemoAuthController] Demo user not found. Check DemoUserSeeder."));
        return ResponseEntity.ok(userResponse);
    }

    /**
     * POST /api/auth/reset-password — no-op in demo mode.
     */
    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(@RequestBody(required = false) Map<String, Object> req) {
        return ResponseEntity.ok(Map.of("message", "Password reset successfully."));
    }

    // ── Private Helpers ──────────────────────────────────────────────────────

    private void addDemoCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie("prepcreatine_token", "demo-token-arjun");
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(7 * 24 * 3600); // 7 days
        response.addCookie(cookie);
    }

    private Map<String, Object> buildDemoLoginResponse() {
        return Map.of(
                "userId",            DemoModeConfig.DEMO_USER_ID.toString(),
                "email",             "arjun@prepcreatine.demo",
                "fullName",          "Arjun Sharma",
                "role",              "STUDENT",
                "onboardingComplete", true,
                "emailVerified",     true,
                "examType",          "jee"
        );
    }
}
