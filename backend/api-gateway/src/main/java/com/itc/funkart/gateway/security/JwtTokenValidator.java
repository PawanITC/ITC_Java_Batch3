package com.itc.funkart.gateway.security;

import com.itc.funkart.gateway.config.JwtConfig;
import com.itc.funkart.gateway.exception.OAuthException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Base64;

/**
 * Validates JWT tokens received from user-service.
 * Does NOT generate tokens (that's user-service's job).
 */
@Component
public class JwtTokenValidator {

    private final SecretKey key;

    public JwtTokenValidator(JwtConfig jwtConfig) {
        byte[] keyBytes = Base64.getDecoder().decode(jwtConfig.getSecret());
        this.key = Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Parse and validate JWT claims
     */
    public Claims validateAndParseClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (Exception e) {
            // Wrap JJWT technical errors into our domain's OAuthException
            throw new OAuthException("Token validation failed: " + e.getMessage(), e);
        }

    }
}