package com.scalemart.api.repository;

import com.scalemart.api.domain.TokenBlacklist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface TokenBlacklistRepository extends JpaRepository<TokenBlacklist, Long> {
    boolean existsByToken(String token);
    
    @Modifying
    @Query("DELETE FROM TokenBlacklist t WHERE t.expiresAt < CURRENT_TIMESTAMP")
    int deleteExpiredTokens();
}
