package com.bunsen.api.aftercare.repository;

import com.bunsen.api.aftercare.model.BlacklistedToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface BlacklistedTokenRepository extends JpaRepository<BlacklistedToken, String> {
    @Query("DELETE FROM BlacklistedToken b WHERE b.expiresAt < :now")
    @Modifying
    int deleteExpiredTokens(@Param("now") LocalDateTime now);
}
