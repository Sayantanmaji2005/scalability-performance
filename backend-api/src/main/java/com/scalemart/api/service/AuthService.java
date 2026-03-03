package com.scalemart.api.service;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.scalemart.api.config.BusinessMetrics;
import com.scalemart.api.config.JwtService;
import com.scalemart.api.domain.RefreshToken;
import com.scalemart.api.domain.User;
import com.scalemart.api.dto.AuthActionResponse;
import com.scalemart.api.dto.ChangePasswordRequest;
import com.scalemart.api.dto.ForgotPasswordRequest;
import com.scalemart.api.dto.LoginResponse;
import com.scalemart.api.dto.RefreshTokenRequest;
import com.scalemart.api.dto.RegisterRequest;
import com.scalemart.api.dto.ResendVerificationRequest;
import com.scalemart.api.dto.ResetPasswordRequest;
import com.scalemart.api.dto.VerifyEmailRequest;
import com.scalemart.api.repository.UserRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Locale;
import java.util.Optional;
import java.security.SecureRandom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private static final Duration EMAIL_VERIFICATION_TOKEN_TTL = Duration.ofHours(24);
    private static final Duration PASSWORD_RESET_TOKEN_TTL = Duration.ofMinutes(30);
    private static final int TOKEN_SIZE_BYTES = 32;

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final TokenBlacklistService tokenBlacklistService;
    private final UserRepository userRepository;
    private final BusinessMetrics businessMetrics;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final boolean exposeDebugTokens;
    private final SecureRandom secureRandom = new SecureRandom();

    public AuthService(
        AuthenticationManager authenticationManager,
        JwtService jwtService,
        RefreshTokenService refreshTokenService,
        TokenBlacklistService tokenBlacklistService,
        UserRepository userRepository,
        BusinessMetrics businessMetrics,
        PasswordEncoder passwordEncoder,
        EmailService emailService,
        @Value("${app.auth.expose-debug-tokens:false}") boolean exposeDebugTokens) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
        this.tokenBlacklistService = tokenBlacklistService;
        this.userRepository = userRepository;
        this.businessMetrics = businessMetrics;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.exposeDebugTokens = exposeDebugTokens;
    }

    public LoginResponse login(String username, String password) {
        long startTime = System.currentTimeMillis();
        String normalizedUsername = normalizeUsername(username);
        
        try {
            authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(normalizedUsername, password)
            );

            User user = userRepository.findByUsername(normalizedUsername)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + normalizedUsername));
            if (!user.isEmailVerified()) {
                throw new IllegalArgumentException("Email is not verified. Verify your email before login.");
            }

            String token = jwtService.generateToken(normalizedUsername);
            String refreshToken = jwtService.generateRefreshToken(normalizedUsername);
            refreshTokenService.createRefreshToken(normalizedUsername);

            long expiresAtEpochSeconds = jwtService.extractExpiry(token).getEpochSecond();
            long processingTime = System.currentTimeMillis() - startTime;
            
            // Record successful login metrics
            businessMetrics.recordLoginSuccess(processingTime);

            return new LoginResponse(
                token,
                refreshToken,
                expiresAtEpochSeconds,
                user.getRole()
            );
        } catch (BadCredentialsException e) {
            // Record failed login metrics
            businessMetrics.recordLoginFailure();
            throw e;
        }
    }

    public LoginResponse refreshToken(RefreshTokenRequest request) {
        // Verify the refresh token
        RefreshToken refreshToken = refreshTokenService.verifyRefreshToken(request.refreshToken());
        
        String username = refreshToken.getUser().getUsername();
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
        if (!user.isEmailVerified()) {
            throw new IllegalArgumentException("Email is not verified. Verify your email before refreshing session.");
        }
        
        // Generate new access token
        String newAccessToken = jwtService.generateToken(username);
        
        // Generate new refresh token (rotation for security)
        refreshTokenService.revokeRefreshToken(request.refreshToken());
        String newRefreshToken = jwtService.generateRefreshToken(username);
        refreshTokenService.createRefreshToken(username);

        // Record token refresh metrics
        businessMetrics.recordTokenRefresh();

        long expiresAtEpochSeconds = jwtService.extractExpiry(newAccessToken).getEpochSecond();

        return new LoginResponse(
            newAccessToken,
            newRefreshToken,
            expiresAtEpochSeconds,
            user.getRole()
        );
    }

    public AuthActionResponse register(RegisterRequest request) {
        String username = normalizeUsername(request.username());
        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("Username already exists");
        }

        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setEmail(username);
        user.setRole("USER");
        user.setEnabled(true);
        user.setEmailVerified(false);
        String verificationToken = generateToken();
        user.setEmailVerificationTokenHash(hashToken(verificationToken));
        user.setEmailVerificationExpiresAt(Instant.now().plus(EMAIL_VERIFICATION_TOKEN_TTL));
        user.setPasswordResetTokenHash(null);
        user.setPasswordResetExpiresAt(null);
        userRepository.save(user);

        boolean delivered = emailService.sendVerificationEmail(user.getEmail(), username, verificationToken);
        String message = delivered
            ? "Account created. Check your email for verification."
            : "Account created. Email delivery unavailable. Use verification token in UI.";

        if (exposeDebugTokens) {
            log.info("Email verification token for {}: {}", username, verificationToken);
        }
        return new AuthActionResponse(
            message,
            exposeDebugTokens ? verificationToken : null
        );
    }

    public AuthActionResponse verifyEmail(VerifyEmailRequest request) {
        String username = normalizeUsername(request.username());
        String token = request.token().trim();
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new IllegalArgumentException("Invalid verification request"));

        if (user.isEmailVerified()) {
            return new AuthActionResponse("Email is already verified. You can login.", null);
        }

        if (!isTokenValid(user.getEmailVerificationTokenHash(), user.getEmailVerificationExpiresAt(), token)) {
            throw new IllegalArgumentException("Verification token is invalid or expired");
        }

        user.setEmailVerified(true);
        user.setEmailVerificationTokenHash(null);
        user.setEmailVerificationExpiresAt(null);
        userRepository.save(user);
        return new AuthActionResponse("Email verified successfully. You can login now.", null);
    }

    public AuthActionResponse resendVerification(ResendVerificationRequest request) {
        String username = normalizeUsername(request.username());
        Optional<User> optionalUser = userRepository.findByUsername(username);
        if (optionalUser.isEmpty()) {
            return new AuthActionResponse(
                "If an unverified account exists, verification instructions were generated.",
                null
            );
        }

        User user = optionalUser.get();
        if (user.isEmailVerified()) {
            return new AuthActionResponse("Email is already verified. You can login.", null);
        }

        String verificationToken = generateToken();
        user.setEmailVerificationTokenHash(hashToken(verificationToken));
        user.setEmailVerificationExpiresAt(Instant.now().plus(EMAIL_VERIFICATION_TOKEN_TTL));
        userRepository.save(user);

        boolean delivered = emailService.sendVerificationEmail(user.getEmail(), username, verificationToken);
        String message = delivered
            ? "Verification instructions sent. Check your email."
            : "Email delivery unavailable. Use verification token in UI.";

        if (exposeDebugTokens) {
            log.info("Resent verification token for {}: {}", username, verificationToken);
        }
        return new AuthActionResponse(message, exposeDebugTokens ? verificationToken : null);
    }

    public AuthActionResponse forgotPassword(ForgotPasswordRequest request) {
        String username = normalizeUsername(request.username());
        Optional<User> optionalUser = userRepository.findByUsername(username);

        String resetToken = null;
        if (optionalUser.isPresent()) {
            User user = optionalUser.get();
            resetToken = generateToken();
            user.setPasswordResetTokenHash(hashToken(resetToken));
            user.setPasswordResetExpiresAt(Instant.now().plus(PASSWORD_RESET_TOKEN_TTL));
            userRepository.save(user);
            boolean delivered = emailService.sendPasswordResetEmail(user.getEmail(), username, resetToken);
            if (!delivered) {
                log.info("Password reset email delivery unavailable for {}", username);
            }
            if (exposeDebugTokens) {
                log.info("Password reset token for {}: {}", username, resetToken);
            }
        }

        return new AuthActionResponse(
            "If an account exists, password reset instructions were generated.",
            exposeDebugTokens ? resetToken : null
        );
    }

    public AuthActionResponse resetPassword(ResetPasswordRequest request) {
        String username = normalizeUsername(request.username());
        String token = request.token().trim();
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new IllegalArgumentException("Invalid reset token or user"));

        if (!isTokenValid(user.getPasswordResetTokenHash(), user.getPasswordResetExpiresAt(), token)) {
            throw new IllegalArgumentException("Reset token is invalid or expired");
        }

        user.setPassword(passwordEncoder.encode(request.newPassword()));
        user.setPasswordResetTokenHash(null);
        user.setPasswordResetExpiresAt(null);
        userRepository.save(user);
        refreshTokenService.revokeAllUserTokens(username);
        return new AuthActionResponse("Password reset successful. Please login with your new password.", null);
    }

    public AuthActionResponse changePassword(String authenticatedUsername, ChangePasswordRequest request) {
        String username = normalizeUsername(authenticatedUsername);

        try {
            authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(username, request.currentPassword())
            );
        } catch (BadCredentialsException exception) {
            throw new IllegalArgumentException("Current password is incorrect");
        }

        if (request.currentPassword().equals(request.newPassword())) {
            throw new IllegalArgumentException("New password must be different from current password");
        }

        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));
        user.setPassword(passwordEncoder.encode(request.newPassword()));
        user.setPasswordResetTokenHash(null);
        user.setPasswordResetExpiresAt(null);
        userRepository.save(user);

        refreshTokenService.revokeAllUserTokens(username);
        return new AuthActionResponse("Password changed successfully.", null);
    }

    public void logout(String token, String username) {
        // Add to blacklist
        tokenBlacklistService.addToBlacklist(token, jwtService.extractExpiry(token));
        
        // Revoke all refresh tokens
        refreshTokenService.revokeAllUserTokens(username);
        
        // Record logout metrics
        businessMetrics.recordLogout();
    }

    private String normalizeUsername(String username) {
        if (username == null) {
            throw new IllegalArgumentException("Username is required");
        }

        String normalized = username.trim().toLowerCase(Locale.ROOT);
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("Username is required");
        }
        return normalized;
    }

    private String generateToken() {
        byte[] bytes = new byte[TOKEN_SIZE_BYTES];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(hashed.length * 2);
            for (byte b : hashed) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }

    private boolean isTokenValid(String expectedHash, Instant expiry, String presentedToken) {
        if (expectedHash == null || expiry == null || presentedToken == null || presentedToken.isBlank()) {
            return false;
        }
        if (expiry.isBefore(Instant.now())) {
            return false;
        }
        String actualHash = hashToken(presentedToken.trim());
        return expectedHash.equals(actualHash);
    }
}
