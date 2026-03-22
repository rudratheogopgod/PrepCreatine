package com.prepcreatine.service;

import com.prepcreatine.domain.EmailVerificationToken;
import com.prepcreatine.domain.User;
import com.prepcreatine.dto.request.*;
import com.prepcreatine.dto.response.UserResponse;
import com.prepcreatine.exception.*;
import com.prepcreatine.repository.EmailVerificationTokenRepository;
import com.prepcreatine.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;

/**
 * Authentication service per BSDD §8.
 * Handles: signup, login, token refresh, email verification,
 *          forgot password, reset password, change password.
 *
 * [SECURITY] Login failure always returns the same generic message
 * to prevent email enumeration.
 */
@Service
@Transactional
public class AuthService {

    private static final Logger     log    = LoggerFactory.getLogger(AuthService.class);
    private static final SecureRandom RNG  = new SecureRandom();
    private static final int TOKEN_BYTES   = 32;

    private final UserRepository               userRepo;
    private final EmailVerificationTokenRepository tokenRepo;
    private final PasswordEncoder              pw;
    private final JwtService                   jwt;
    private final UserMapper                   mapper;
    private final EmailService                 emailService;

    public AuthService(UserRepository userRepo,
                       EmailVerificationTokenRepository tokenRepo,
                       PasswordEncoder pw,
                       JwtService jwt,
                       UserMapper mapper,
                       EmailService emailService) {
        this.userRepo     = userRepo;
        this.tokenRepo    = tokenRepo;
        this.pw           = pw;
        this.jwt          = jwt;
        this.mapper       = mapper;
        this.emailService = emailService;
    }

    // ── Signup ─────────────────────────────────────────────────────────────

    public Map<String, Object> signup(SignupRequest req) {
        String email = req.email().toLowerCase().trim();

        if (userRepo.existsByEmail(email)) {
            throw new DuplicateResourceException("An account with this email already exists.");
        }

        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(pw.encode(req.password()));
        user.setFullName(req.fullName().trim());
        user.setRole("STUDENT");
        user.setShareToken(generateToken());
        userRepo.save(user);

        // Send verification email async
        sendVerificationEmail(user);

        return tokenPair(user);
    }

    // ── Login ─────────────────────────────────────────────────────────────

    public Map<String, Object> login(LoginRequest req) {
        String email = req.email().toLowerCase().trim();
        // [SECURITY] same error for both unknown email AND wrong password
        User user = userRepo.findByEmailAndIsActiveTrue(email)
            .orElseThrow(() -> new UnauthorizedException("Invalid email or password."));

        if (user.getPasswordHash() == null) {
            // Google-only account — no password set
            throw new UnauthorizedException("Invalid email or password.");
        }

        if (!pw.matches(req.password(), user.getPasswordHash())) {
            throw new UnauthorizedException("Invalid email or password.");
        }

        return tokenPair(user);
    }

    // ── Token Refresh ──────────────────────────────────────────────────────

    public Map<String, Object> refresh(String refreshToken) {
        var claims = jwt.validateAndExtract(refreshToken)
            .filter(c -> "refresh".equals(jwt.extractType(c)))
            .orElseThrow(() -> new UnauthorizedException("Invalid or expired refresh token."));

        UUID userId = jwt.extractUserId(claims);
        User user   = userRepo.findByIdAndIsActiveTrue(userId)
            .orElseThrow(() -> new UnauthorizedException("User not found."));

        return tokenPair(user);
    }

    // ── Email Verification ─────────────────────────────────────────────────

    public void verifyEmail(VerifyEmailRequest req) {
        EmailVerificationToken token = tokenRepo.findByTokenAndType(req.token(), "verify_email")
            .orElseThrow(() -> new ValidationException("Invalid or expired verification link."));

        if (!token.isValid()) {
            throw new ValidationException("This verification link has expired. Please request a new one.");
        }

        User user = userRepo.findById(token.getUserId())
            .orElseThrow(() -> new ResourceNotFoundException("User not found."));
        user.setEmailVerified(true);
        userRepo.save(user);

        token.setUsedAt(OffsetDateTime.now());
        tokenRepo.save(token);
    }

    public void resendVerificationEmail(UUID userId) {
        User user = userRepo.findByIdAndIsActiveTrue(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found."));
        if (user.isEmailVerified()) {
            throw new ValidationException("Email is already verified.");
        }
        sendVerificationEmail(user);
    }

    // ── Forgot / Reset Password ───────────────────────────────────────────

    /**
     * [SECURITY] Always returns OK — no indication whether email exists.
     */
    public void forgotPassword(ForgotPasswordRequest req) {
        String email = req.email().toLowerCase().trim();
        userRepo.findByEmailAndIsActiveTrue(email).ifPresent(user -> {
            tokenRepo.invalidateExistingTokens(user.getId(), "password_reset");
            sendPasswordResetEmail(user);
        });
    }

    public void resetPassword(ResetPasswordRequest req) {
        if (!req.newPassword().equals(req.confirmPassword())) {
            throw new ValidationException("Passwords do not match.");
        }

        EmailVerificationToken token = tokenRepo.findByTokenAndType(req.token(), "password_reset")
            .orElseThrow(() -> new ValidationException("Invalid or expired reset link."));

        if (!token.isValid()) {
            throw new ValidationException("This reset link has expired. Please request a new one.");
        }

        User user = userRepo.findById(token.getUserId())
            .orElseThrow(() -> new ResourceNotFoundException("User not found."));
        user.setPasswordHash(pw.encode(req.newPassword()));
        userRepo.save(user);

        token.setUsedAt(OffsetDateTime.now());
        tokenRepo.save(token);
        log.info("[Auth] Password reset for userId={}", user.getId());
    }

    public void changePassword(ChangePasswordRequest req, UUID userId) {
        if (!req.newPassword().equals(req.confirmPassword())) {
            throw new ValidationException("Passwords do not match.");
        }
        User user = userRepo.findByIdAndIsActiveTrue(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found."));

        if (!pw.matches(req.currentPassword(), user.getPasswordHash())) {
            throw new ValidationException("Current password is incorrect.");
        }
        user.setPasswordHash(pw.encode(req.newPassword()));
        userRepo.save(user);
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private Map<String, Object> tokenPair(User user) {
        String accessToken  = jwt.generateAccessToken(user.getId(), user.getEmail(), user.getRole());
        String refreshToken = jwt.generateRefreshToken(user.getId());
        return Map.of(
            "accessToken",  accessToken,
            "refreshToken", refreshToken,
            "user",         mapper.toResponse(user)
        );
    }

    private void sendVerificationEmail(User user) {
        tokenRepo.invalidateExistingTokens(user.getId(), "verify_email");
        EmailVerificationToken token = new EmailVerificationToken();
        token.setUserId(user.getId());
        token.setToken(generateToken());
        token.setType("verify_email");
        token.setExpiresAt(OffsetDateTime.now().plusHours(24));
        tokenRepo.save(token);
        emailService.sendVerificationEmail(user.getEmail(), user.getFullName(), token.getToken());
    }

    private void sendPasswordResetEmail(User user) {
        EmailVerificationToken token = new EmailVerificationToken();
        token.setUserId(user.getId());
        token.setToken(generateToken());
        token.setType("password_reset");
        token.setExpiresAt(OffsetDateTime.now().plusHours(1));
        tokenRepo.save(token);
        emailService.sendPasswordResetEmail(user.getEmail(), user.getFullName(), token.getToken());
    }

    private static String generateToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        RNG.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
