package com.itc.funkart.gateway.security;

import com.itc.funkart.gateway.config.AppConfig;
import com.itc.funkart.gateway.exception.OAuthException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Base64;

/**
 * Validates JWT tokens received from the user-service.
 * This component is responsible for cryptographic signature verification
 * using the shared secret key.
 */
@Component
public class JwtTokenValidator {

    private static final Logger log = LoggerFactory.getLogger(JwtTokenValidator.class);
    private final SecretKey key;

    public JwtTokenValidator(AppConfig appConfig) {
        try {
            byte[] keyBytes = Base64.getDecoder().decode(appConfig.jwt().secret());
            this.key = Keys.hmacShaKeyFor(keyBytes);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("JWT Secret is not a valid Base64 string. Check your configuration.", e);
        }
    }

    /**
     * Parse and validate JWT claims.
     * * @param token The JWT string to validate
     * @return Claims extracted from the token if valid
     * @throws OAuthException if the token is malformed, tampered with, or expired
     */
    public Claims validateAndParseClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            throw new OAuthException("Session expired. Please log in again.");
        } catch (MalformedJwtException | SignatureException e) {
            log.error("Possible security tampering detected: {}", e.getMessage());
            throw new OAuthException("Invalid token signature.");
        } catch (Exception e) {
            throw new OAuthException("Token validation failed: " + e.getMessage());
        }
    }
}