package com.bunsen.api.aftercare.security.jwt;

import com.bunsen.api.aftercare.service.DatabaseJwtBlacklistService;
import com.bunsen.api.aftercare.service.UserDetailsImpl;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
public class JwtUtils {
    private static final Logger logger = LoggerFactory.getLogger(JwtUtils.class);
    private final DatabaseJwtBlacklistService databaseJwtBlacklistService;

    @Value("${aftercare.app.jwtSecret}")
    private String jwtSecret;

    @Value("${aftercare.app.jwtExpirationMs}")
    private int jwtExpirationMs;

    @Value("${aftercare.app.jwtRefreshExpirationMs}")
    private int jwtRefreshExpirationMs;

    @Value("${aftercare.app.verificationTokenExpirationMs}")
    private int verificationTokenExpirationMs;

    public JwtUtils(@Lazy DatabaseJwtBlacklistService databaseJwtBlacklistService) {
        this.databaseJwtBlacklistService = databaseJwtBlacklistService;
    }

    public String generateJwtToken(Authentication authentication) {
        UserDetailsImpl userPrincipal = (UserDetailsImpl) authentication.getPrincipal();

        return Jwts.builder()
                .subject(userPrincipal.getUsername())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + jwtExpirationMs))
                .signWith(Keys.hmacShaKeyFor(jwtSecret.getBytes()), Jwts.SIG.HS512)
                .compact();
    }

    public String generateVerificationToken(String username, String purpose) {
        return Jwts.builder()
                .subject(username)
                .claim("purpose", purpose)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + verificationTokenExpirationMs))
                .signWith(Keys.hmacShaKeyFor(jwtSecret.getBytes()), Jwts.SIG.HS512)
                .compact();
    }

    public String generateRefreshToken(Authentication authentication) {
        UserDetailsImpl userPrincipal = (UserDetailsImpl) authentication.getPrincipal();

        return Jwts.builder()
                .subject(userPrincipal.getUsername())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + jwtRefreshExpirationMs))
                .signWith(Keys.hmacShaKeyFor(jwtSecret.getBytes()), Jwts.SIG.HS512)
                .compact();
    }

    public String getUserNameFromJwtToken(String token) {
        return getClaimsFromJwtToken(token).getSubject();
    }

    public String getPurposeFromJwtToken(String token) {
        return getClaimsFromJwtToken(token).get("purpose", String.class);
    }

    public Claims getClaimsFromJwtToken(String token) {
        return Jwts.parser()
                .verifyWith(Keys.hmacShaKeyFor(jwtSecret.getBytes()))
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean validateJwtToken(String authToken) {
        try {

            if (databaseJwtBlacklistService.isTokenBlacklisted(authToken)) {
                logger.error("JWT token is blacklisted");
                return false;
            }

            Jwts.parser()
                    .verifyWith(Keys.hmacShaKeyFor(jwtSecret.getBytes()))
                    .build()
                    .parseSignedClaims(authToken);
            return true;
        } catch (SignatureException e) {
            logger.error("Invalid JWT signature: {}", e.getMessage());
        } catch (MalformedJwtException e) {
            logger.error("Invalid JWT token: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            logger.error("JWT token is expired: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            logger.error("JWT token is unsupported: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            logger.error("JWT claims string is empty: {}", e.getMessage());
        }
        return false;
    }

    public String generatePasswordResetToken(String username) {
        return generateVerificationToken(username, "password_reset");
    }

    public String generateEmailVerificationToken(String username) {
        return generateVerificationToken(username, "email_verification");
    }
}