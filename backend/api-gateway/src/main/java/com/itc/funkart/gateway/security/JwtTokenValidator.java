package com.itc.funkart.gateway.security;

import com.itc.funkart.gateway.config.AppConfig;
import com.itc.funkart.gateway.exception.OAuthException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
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

    private final SecretKey key;

    public JwtTokenValidator(AppConfig appConfig) {
        // Accessing the secret from the nested JWT record
        byte[] keyBytes = Base64.getDecoder().decode(appConfig.jwt().secret());
        this.key = Keys.hmacShaKeyFor(keyBytes);
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
        } catch (Exception e) {
            // Wrap JJWT technical errors into our domain's OAuthException for uniform handling
            throw new OAuthException("Token validation failed: " + e.getMessage(), e);
        }
    }
}