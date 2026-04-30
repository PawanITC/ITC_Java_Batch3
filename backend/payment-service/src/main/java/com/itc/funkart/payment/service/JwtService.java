package com.itc.funkart.payment.service;

import com.itc.funkart.payment.auth.claims.JwtClaims;
import com.itc.funkart.payment.config.JwtConfig;
import com.itc.funkart.payment.dto.jwt.JwtUserDto;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;

/**
 * <h2>JwtService</h2>
 * <p>
 * The cryptographic engine responsible for identity issuance and verification within the FunKart ecosystem.
 * </p>
 * <p>
 * This service centralizes the logic for creating signed JSON Web Tokens and parsing them back into
 * usable claims. It relies on the {@link JwtClaims} constants to maintain a strict contract between
 * the Identity Provider (User Service) and the Resource Servers (Payment, Order services).
 * </p>
 */
@Service
public class JwtService {

    private final JwtConfig jwtConfig;
    private final SecretKey key;

    /**
     * Constructs the service and initializes the HMAC signing key.
     * * @param jwtConfig Configuration containing the Base64 encoded secret and expiration settings.
     */
    public JwtService(JwtConfig jwtConfig) {
        this.jwtConfig = jwtConfig;
        // Using java.util.Base64 to match Gateway
        byte[] keyBytes = java.util.Base64.getDecoder().decode(jwtConfig.getSecret());
        this.key = Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Generates a signed JWT for a verified user.
     * <p>
     * <b>Claim Architecture:</b>
     * <ul>
     * <li>{@code sub}: The unique User ID.</li>
     * <li>{@code iss}: The centralized authority name defined in {@link JwtClaims#ISSUER}.</li>
     * <li>{@code role}: The user's security authority (e.g., ROLE_USER, ROLE_ADMIN).</li>
     * </ul>
     * </p>
     *
     * @param user The data transfer object representing the authenticated user.
     * @return A compact, signed JWT string.
     */
    public String generateJwtToken(JwtUserDto user) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtConfig.getExpirationMs());

        return Jwts.builder()
                .subject(user.id().toString())
                .issuer(JwtClaims.ISSUER)
                .claim(JwtClaims.NAME, user.name())
                .claim(JwtClaims.EMAIL, user.email())
                .claim(JwtClaims.ROLE, user.role())
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(key)
                .compact();
    }

    /**
     * Validates and parses the claims from a provided JWT string.
     * <p>
     * This method performs cryptographic signature verification. If the token has been
     * modified since issuance, an exception will be thrown.
     * </p>
     *
     * @param token The raw JWT string.
     * @return The extracted {@link Claims} payload.
     * @throws io.jsonwebtoken.JwtException If the token signature is invalid or the token is malformed.
     */
    public Claims parseJwtToken(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .requireIssuer(JwtClaims.ISSUER)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Checks if a token is authentic and has not expired.
     *
     * @param token The JWT string to validate.
     * @return {@code true} if valid and active; {@code false} if expired or tampered with.
     */
    public boolean validateToken(String token) {
        try {
            Claims claims = parseJwtToken(token);
            return !claims.getExpiration().before(new Date());
        } catch (Exception e) {
            return false;
        }
    }
}