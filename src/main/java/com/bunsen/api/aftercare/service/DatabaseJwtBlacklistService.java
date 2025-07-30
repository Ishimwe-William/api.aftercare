package com.bunsen.api.aftercare.service;

import com.bunsen.api.aftercare.model.BlacklistedToken;
import com.bunsen.api.aftercare.repository.BlacklistedTokenRepository;
import com.bunsen.api.aftercare.security.jwt.JwtUtils;
import io.jsonwebtoken.Claims;
import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

@Service
public class DatabaseJwtBlacklistService {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseJwtBlacklistService.class);

    private final BlacklistedTokenRepository repository;
    private final JwtUtils jwtUtils;

    public DatabaseJwtBlacklistService(BlacklistedTokenRepository repository, JwtUtils jwtUtils) {
        this.repository = repository;
        this.jwtUtils = jwtUtils;
    }

    public void blacklistToken(String token) {
        try {
            // Generate a hash of the token to avoid storing the full token
            String tokenHash = DigestUtils.sha256Hex(token);

            Claims claims = jwtUtils.getClaimsFromJwtToken(token);
            Date expiration = claims.getExpiration();

            BlacklistedToken blacklistedToken = new BlacklistedToken();
            blacklistedToken.setTokenId(tokenHash);
            blacklistedToken.setBlacklistedAt(LocalDateTime.now());
            blacklistedToken.setExpiresAt(expiration.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime());

            repository.save(blacklistedToken);
        } catch (Exception e) {
            logger.error("Failed to blacklist token in database: {}", e.getMessage());
        }
    }

    public boolean isTokenBlacklisted(String token) {
        String tokenHash = DigestUtils.sha256Hex(token);
        return repository.existsById(tokenHash);
    }

    @Scheduled(cron = "0 0 * * * *") // Run every hour
    public void cleanupExpiredTokens() {
        int deleted = repository.deleteExpiredTokens(LocalDateTime.now());
        logger.info("Cleaned up {} expired blacklisted tokens", deleted);
    }
}
