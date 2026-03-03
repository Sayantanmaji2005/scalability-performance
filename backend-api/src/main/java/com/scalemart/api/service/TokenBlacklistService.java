package com.scalemart.api.service;

import com.scalemart.api.domain.TokenBlacklist;
import com.scalemart.api.repository.TokenBlacklistRepository;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TokenBlacklistService {

    private static final Logger log = LoggerFactory.getLogger(TokenBlacklistService.class);

    private final TokenBlacklistRepository tokenBlacklistRepository;

    public TokenBlacklistService(TokenBlacklistRepository tokenBlacklistRepository) {
        this.tokenBlacklistRepository = tokenBlacklistRepository;
    }

    @Transactional
    public void addToBlacklist(String token, Instant expirationTime) {
        if (tokenBlacklistRepository.existsByToken(token)) {
            log.debug("Token already in blacklist");
            return;
        }

        TokenBlacklist blacklistEntry = new TokenBlacklist();
        blacklistEntry.setToken(token);
        blacklistEntry.setExpiresAt(expirationTime);
        
        tokenBlacklistRepository.save(blacklistEntry);
        log.info("Token added to blacklist");
    }

    public boolean isBlacklisted(String token) {
        return tokenBlacklistRepository.existsByToken(token);
    }

    @Transactional
    public int cleanupExpiredTokens() {
        int deleted = tokenBlacklistRepository.deleteExpiredTokens();
        if (deleted > 0) {
            log.info("Cleaned up {} expired blacklist entries", deleted);
        }
        return deleted;
    }
}
