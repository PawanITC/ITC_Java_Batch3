package com.itc.funkart.gateway.service;

import com.itc.funkart.common.constants.auth.JwtClaims;
import com.itc.funkart.gateway.config.AppConfig;
import com.itc.funkart.gateway.exception.JwtAuthenticationException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.Date;

/**
 * <h2>JWT Service (The Cryptographic Engine)</h2>
 * <p>
 * This service handles the lifecycle of JSON Web Tokens. It acts as the
 * primary security mechanism for the "Operating Environment" (Security Context).
 * </p>
 */
@Service
public class JwtService {

    private static final long CLOCK_SKEW_SECONDS = 30;
    private final SecretKey key;

    public JwtService(AppConfig appConfig) {
        String rawSecret = appConfig.jwt().secret();
        if (rawSecret == null || rawSecret.isBlank()) {
            throw new IllegalStateException("CRITICAL: JWT Secret is missing from AppConfig!");
        }

        byte[] keyBytes = Base64.getDecoder().decode(rawSecret.trim());
        this.key = Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Parses and validates a JWT string.
     * <p>Note: JJWT automatically validates the 'exp' (expiration) and 'iss' (issuer)
     * during the parseSignedClaims call.</p>
     *
     * @param token The raw JWT string from the HttpOnly cookie.
     * @return The claims contained within the token.
     * @throws JwtAuthenticationException if the token is expired, invalid, or malformed.
     */
    public Claims parseClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(key)
                    .requireIssuer(JwtClaims.ISSUER)
                    .clockSkewSeconds(CLOCK_SKEW_SECONDS)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

        } catch (ExpiredJwtException e) {
            throw new JwtAuthenticationException("JWT token has expired", e);
        } catch (JwtException | IllegalArgumentException e) {
            throw new JwtAuthenticationException("Invalid or malformed JWT", e);
        }
    }

    /**
     * Safely extracts the User ID from the token claims.
     *
     * @param claims The validated JWT claims.
     * @return The Long representation of the user subject.
     */
    public Long getUserId(Claims claims) {
        try {
            String subject = claims.getSubject();
            if (subject == null) throw new JwtAuthenticationException("JWT subject (User ID) is missing");
            return Long.parseLong(subject);
        } catch (NumberFormatException e) {
            throw new JwtAuthenticationException("JWT subject is not a valid User ID format", e);
        }
    }

    /**
     * Generates a short-lived Access Token.
     *
     * @param userId The database ID of the user.
     * @param role   The user's role (e.g., ROLE_USER).
     * @return A signed JWT string.
     */
    public String generateAccessToken(Long userId, String role) {
        // Implementation detail: Future work should pull 15m from AppConfig
        return Jwts.builder()
                .issuer(JwtClaims.ISSUER)
                .subject(String.valueOf(userId))
                .claim(JwtClaims.ROLE, role)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 1000 * 60 * 15))
                .signWith(key)
                .compact();
    }

    /**
     * Extracts the expiration date from the claims.
     *
     * @param claims The validated JWT claims.
     * @return The Date of expiration.
     */
    public Date getExpiration(Claims claims) {
        Date exp = claims.getExpiration();
        if (exp == null) {
            throw new JwtAuthenticationException("JWT expiration claim is missing");
        }
        return exp;
    }
}