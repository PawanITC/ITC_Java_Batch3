package com.itc.funkart.gateway.security;

import com.itc.funkart.gateway.config.AppConfig;
import com.itc.funkart.gateway.exception.OAuthException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Base64;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit tests for {@link JwtTokenValidator}.
 * <p>
 * These tests ensure that the Gateway can correctly distinguish between
 * valid identity tokens and fraudulent or expired ones. It specifically
 * validates the integration with JJWT and our custom error handling.
 */
public class JwtTokenValidatorTest {

    private JwtTokenValidator validator;
    private final String B64_SECRET = Base64.getEncoder().encodeToString("super-secret-key-must-be-at-least-32-bytes!!".getBytes());

    /**
     * Initializes a fresh validator before each test using a mock secret key.
     */
    @BeforeEach
    void setUp() {
        // Build the nested records required for the root AppConfig
        AppConfig.Api api = new AppConfig.Api("/api/v1");
        AppConfig.Jwt jwt = new AppConfig.Jwt(B64_SECRET, 3600000L, 3600, "token", false);
        AppConfig.Github github = new AppConfig.Github("id", "secret", "uri");
        AppConfig.Services services = new AppConfig.Services("http://user-service");

        // Initialize the root AppConfig
        AppConfig config = new AppConfig(
                "http://localhost:5173",
                api,
                jwt,
                github,
                services
        );

        validator = new JwtTokenValidator(config);
    }

    /**
     * Verifies that a correctly signed, non-expired token is parsed successfully.
     */
    @Test
    @DisplayName("Valid Token: Should return correct claims")
    void whenValidToken_thenReturnClaims() {
        String token = Jwts.builder()
                .subject("funkart_user")
                .signWith(Keys.hmacShaKeyFor(Base64.getDecoder().decode(B64_SECRET)))
                .compact();

        Claims claims = validator.validateAndParseClaims(token);

        assertEquals("funkart_user", claims.getSubject());
    }

    /**
     * Verifies that tokens past their expiration date are rejected.
     * <p>
     * Note: Expects {@link OAuthException} if the validator wraps
     * JJWT's ExpiredJwtException.
     */
    @Test
    @DisplayName("Expired Token: Should throw OAuthException")
    void whenExpiredToken_thenThrowException() {
        String expiredToken = Jwts.builder()
                .subject("funkart_user")
                .expiration(new Date(System.currentTimeMillis() - 1000))
                .signWith(Keys.hmacShaKeyFor(Base64.getDecoder().decode(B64_SECRET)))
                .compact();

        // Testing for our custom exception
        assertThrows(OAuthException.class, () -> validator.validateAndParseClaims(expiredToken));
    }

    /**
     * Verifies that tokens with modified payloads or signatures are rejected.
     */
    @Test
    @DisplayName("Tampered Token: Should throw OAuthException")
    void whenTamperedToken_thenThrowSignatureException() {
        String token = Jwts.builder()
                .subject("funkart_user")
                .signWith(Keys.hmacShaKeyFor(Base64.getDecoder().decode(B64_SECRET)))
                .compact();

        String tamperedToken = token + "modified";

        // Testing for our custom exception
        assertThrows(OAuthException.class, () -> validator.validateAndParseClaims(tamperedToken));
    }
}