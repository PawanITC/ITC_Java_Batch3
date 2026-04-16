package com.itc.funkart.gateway.security;

import com.itc.funkart.gateway.config.AppConfig;
import com.itc.funkart.gateway.config.props.ApiProperties;
import com.itc.funkart.gateway.exception.OAuthException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Base64;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link JwtTokenValidator}.
 *
 * <p>
 * These tests ensure that the Gateway can correctly distinguish between
 * valid identity tokens and fraudulent or expired ones. It specifically
 * validates the integration with JJWT and our custom error handling.
 * </p>
 */
class JwtTokenValidatorTest {

    private JwtTokenValidator validator;
    private ApiProperties apiProperties;
    private final String B64_SECRET =
            Base64.getEncoder().encodeToString(
                    "super-secret-key-must-be-at-least-32-bytes!!".getBytes()
            );

    /**
     * Initializes a fresh validator before each test using a mock secret key.
     */
    @BeforeEach
    void setUp() {

        AppConfig.Jwt jwt = new AppConfig.Jwt(
                B64_SECRET,
                3600000L,
                3600,
                "token",
                false
        );

        AppConfig config = new AppConfig(
                "http://localhost:5173",
                jwt,
                new AppConfig.Github("id", "secret", "uri"),
                Map.of("user-service", "http://user-service")
        );

        validator = new JwtTokenValidator(config);
    }

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

    @Test
    @DisplayName("Expired Token: Should throw OAuthException")
    void whenExpiredToken_thenThrowException() {

        String expiredToken = Jwts.builder()
                .subject("funkart_user")
                .expiration(new java.util.Date(System.currentTimeMillis() - 1000))
                .signWith(Keys.hmacShaKeyFor(Base64.getDecoder().decode(B64_SECRET)))
                .compact();

        assertThrows(OAuthException.class,
                () -> validator.validateAndParseClaims(expiredToken));
    }

    @Test
    @DisplayName("Tampered Token: Should throw OAuthException")
    void whenTamperedToken_thenThrowSignatureException() {

        String token = Jwts.builder()
                .subject("funkart_user")
                .signWith(Keys.hmacShaKeyFor(Base64.getDecoder().decode(B64_SECRET)))
                .compact();

        assertThrows(OAuthException.class,
                () -> validator.validateAndParseClaims(token + "x"));
    }
}