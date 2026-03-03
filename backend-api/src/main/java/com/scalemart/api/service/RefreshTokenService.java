package com.scalemart.api.service;

import com.scalemart.api.domain.RefreshToken;
import com.scalemart.api.domain.User;
import com.scalemart.api.repository.RefreshTokenRepository;
import com.scalemart.api.repository.UserRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RefreshTokenService {

    private static final Logger log = LoggerFactory.getLogger(RefreshTokenService.class);

    @Value("${app.jwt.refresh-expiration-days:7}")
    private int refreshTokenExpirationDays;

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;

    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository, UserRepository userRepository) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public RefreshToken createRefreshToken(String username) {
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        // Revoke all existing refresh tokens for this user
        refreshTokenRepository.revokeAllByUser(user);

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setToken(UUID.randomUUID().toString());
        refreshToken.setUser(user);
        refreshToken.setExpiresAt(Instant.now().plus(refreshTokenExpirationDays, ChronoUnit.DAYS));
        refreshToken.setRevoked(false);

        return refreshTokenRepository.save(refreshToken);
    }

    public RefreshToken verifyRefreshToken(String token) {
        return refreshTokenRepository.findByToken(token)
            .filter(rt -> !rt.isRevoked() && rt.getExpiresAt().isAfter(Instant.now()))
            .orElseThrow(() -> new IllegalArgumentException("Invalid or expired refresh token"));
    }

    @Transactional
    public void revokeRefreshToken(String token) {
        refreshTokenRepository.findByToken(token)
            .ifPresent(rt -> {
                rt.setRevoked(true);
                refreshTokenRepository.save(rt);
            });
    }

    @Transactional
    public void revokeAllUserTokens(String username) {
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
        refreshTokenRepository.revokeAllByUser(user);
    }

    public int cleanupExpiredTokens() {
        int deleted = refreshTokenRepository.deleteExpiredTokens();
        if (deleted > 0) {
            log.info("Cleaned up {} expired refresh tokens", deleted);
        }
        return deleted;
    }
}
