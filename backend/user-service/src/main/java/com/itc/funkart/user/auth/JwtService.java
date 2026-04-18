package com.itc.funkart.user.auth;

import com.itc.funkart.user.auth.jwt.JwtClaims;
import com.itc.funkart.user.config.JwtConfig;
import com.itc.funkart.user.dto.security.UserPrincipalDto;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.Date;

/**
 * <h2>JWT Service</h2>
 *
 * <p>
 * Responsible for creation, signing, parsing, and validation of JSON Web Tokens (JWTs).
 * This service is owned by the <b>user-service</b>, which is the system of record for authentication.
 * </p>
 *
 * <p>
 * The API Gateway does NOT generate or mutate tokens — it only forwards or validates them.
 * </p>
 *
 * <h3>Responsibilities:</h3>
 * <ul>
 *   <li>Generate signed JWTs from authenticated user identity</li>
 *   <li>Parse and validate incoming JWTs</li>
 *   <li>Extract claims from verified tokens</li>
 * </ul>
 */
@Service
public class JwtService {

    private static final String ISSUER = "funkart-user-service";

    private final JwtConfig jwtConfig;
    private final SecretKey key;

    /**
     * Initializes the signing key from a Base64-encoded secret.
     *
     * @param jwtConfig configuration containing JWT secret and expiration settings
     */
    public JwtService(JwtConfig jwtConfig) {
        this.jwtConfig = jwtConfig;

        byte[] keyBytes = Base64.getDecoder().decode(jwtConfig.getSecret());
        this.key = Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Generates a signed JWT token containing the authenticated user's identity.
     *
     * <p>
     * The token includes only identity and authorization metadata required by downstream services.
     * It intentionally excludes domain relationships (e.g., OAuth providers).
     * </p>
     *
     * @param user authenticated user principal
     * @return compact signed JWT string
     */
    public String generateJwtToken(UserPrincipalDto user) {

        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtConfig.getExpirationMs());

        return Jwts.builder()
                .subject(user.userId().toString())
                .issuer(ISSUER)
                .claim(JwtClaims.NAME, user.name())
                .claim(JwtClaims.EMAIL, user.email())
                .claim(JwtClaims.ROLE, user.role())
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(key)
                .compact();
    }

    /**
     * Parses and validates a JWT token signature.
     *
     * <p>
     * This method does NOT check authorization logic — only structural integrity and signature validity.
     * </p>
     *
     * @param token JWT string
     * @return decoded claims if valid
     * @throws io.jsonwebtoken.JwtException if token is invalid, expired, or tampered with
     */
    public Claims parseJwtToken(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .requireIssuer(ISSUER)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Validates whether a token is structurally valid and not expired.
     *
     * @param token JWT string
     * @return true if valid and not expired, false otherwise
     */
    public boolean validateToken(String token) {
        try {
            Claims claims = parseJwtToken(token);
            return claims.getExpiration().after(new Date());
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Extracts the user ID from a validated JWT token.
     *
     * @param token JWT string
     * @return user ID encoded in the subject claim
     */
    public Long getUserIdFromToken(String token) {
        return Long.parseLong(parseJwtToken(token).getSubject());
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}