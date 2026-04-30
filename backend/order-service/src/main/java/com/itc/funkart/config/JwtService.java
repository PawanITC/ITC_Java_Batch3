package com.itc.funkart.config;

import com.itc.funkart.constants.JwtClaims;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.Date;

/**
 * <h2>JwtService</h2>
 * <p>
 * Provides cryptographic operations for JWT validation. This service is designed to be
 * stateless, using a shared secret to verify tokens issued across the FunKart ecosystem.
 * </p>
 */
@Service
@Slf4j
public class JwtService {

    private final SecretKey key;

    /**
     * Initializes the service by decoding the Base64 secret key from the configuration.
     * * @param jwtConfig Externalized JWT properties (Secret, Expiration).
     */
    public JwtService(JwtConfig jwtConfig) {
        try {
            byte[] keyBytes = Base64.getDecoder().decode(jwtConfig.getSecret());
            this.key = Keys.hmacShaKeyFor(keyBytes);
        } catch (IllegalArgumentException e) {
            log.error("CRITICAL: JWT Secret is not valid Base64. Check your environment variables.");
            throw e;
        }
    }

    /**
     * Extracts the Subject (User ID) from the token.
     * * @param token Valid JWT string.
     *
     * @return The Long representation of the user's ID.
     */
    public Long extractUserId(String token) {
        String subject = parseJwtToken(token).getSubject();
        return Long.parseLong(subject);
    }

    /**
     * Parses the JWT and validates the signature and Issuer.
     * * @param token The raw token string.
     *
     * @return Validated Claims payload.
     */
    public Claims parseJwtToken(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .requireIssuer(JwtClaims.ISSUER) // Ensures the token came from our Auth Authority
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Checks if the token is signature-valid and not expired.
     * * @param token Raw JWT string.
     *
     * @return true if token is usable, false otherwise.
     */
    public boolean validateToken(String token) {
        try {
            Claims claims = parseJwtToken(token);
            return !claims.getExpiration().before(new Date());
        } catch (Exception e) {
            log.debug("Token validation failed for reason: {}", e.getMessage());
            return false;
        }
    }
}