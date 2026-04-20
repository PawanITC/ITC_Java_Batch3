package com.itc.funkart.gateway.service;

import com.itc.funkart.gateway.config.AppConfig;
import com.itc.funkart.gateway.exception.JwtAuthenticationException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * <h2>JwtService — Unit Tests</h2>
 *
 * <p>Covers all public methods of {@link JwtService}:
 * <ul>
 *   <li>{@code parseClaims} — deserialises a signed JWT into its claims payload</li>
 *   <li>{@code validateClaims} — enforces expiration constraints</li>
 *   <li>{@code generateAccessToken} — produces a well-formed, signed JWT</li>
 *   <li>{@code getExpiration} — extracts the expiration date from a raw token</li>
 * </ul>
 *
 * <p>All tests operate against a real HMAC-SHA key so the cryptographic path
 * is exercised end-to-end without any mocking of the JWT library.
 */
class JwtServiceTest {

    /**
     * A 256-bit (32-byte) secret long enough for HS256.
     * Must be at least 32 characters to satisfy JJWT's key length requirement.
     */
    private static final String SECRET =
            "super-secret-key-must-be-at-least-32-bytes!!xyz";

    private JwtService jwtService;
    private SecretKey secretKey;

    /**
     * Builds a minimal {@link AppConfig} record and constructs a fresh
     * {@link JwtService} before every test.
     */
    @BeforeEach
    void setUp() {
        AppConfig.Jwt jwtConfig = new AppConfig.Jwt(
                SECRET,
                3_600_000L,   // 1 hour in ms
                3600,
                "token",
                false
        );

        AppConfig appConfig = new AppConfig(
                "http://localhost:5173",
                jwtConfig,
                new AppConfig.Github("id", "secret", "http://callback"),
                Map.of(
                        "user-service", "http://user-service",
                        "payment-service", "http://payment-service",
                        "order-service", "http://order-service"
                )
        );

        jwtService = new JwtService(appConfig);
        secretKey = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
    }

    // -------------------------------------------------------------------------
    // parseClaims
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("parseClaims")
    class ParseClaimsTests {

        @Test
        @DisplayName("Returns correct subject from a valid token")
        void validToken_returnsCorrectSubject() {
            String token = Jwts.builder()
                    .subject("42")
                    .signWith(secretKey)
                    .compact();

            Claims claims = jwtService.parseClaims(token);

            assertEquals("42", claims.getSubject());
        }

        @Test
        @DisplayName("Returns custom claims (role) from a valid token")
        void validToken_returnsCustomClaims() {
            String token = Jwts.builder()
                    .subject("1")
                    .claim("role", "ROLE_ADMIN")
                    .signWith(secretKey)
                    .compact();

            Claims claims = jwtService.parseClaims(token);

            assertEquals("ROLE_ADMIN", claims.get("role", String.class));
        }

        @Test
        @DisplayName("Throws JwtAuthenticationException for a tampered token")
        void tamperedToken_throwsException() {
            String token = Jwts.builder()
                    .subject("1")
                    .signWith(secretKey)
                    .compact();

            assertThrows(JwtAuthenticationException.class,
                    () -> jwtService.parseClaims(token + "tampered"));
        }

        @Test
        @DisplayName("Throws JwtAuthenticationException for a completely invalid string")
        void invalidString_throwsException() {
            assertThrows(JwtAuthenticationException.class,
                    () -> jwtService.parseClaims("not.a.jwt"));
        }

        @Test
        @DisplayName("Throws JwtAuthenticationException for an expired token")
        void expiredToken_throwsException() {
            String token = Jwts.builder()
                    .subject("1")
                    .expiration(new Date(System.currentTimeMillis() - 10_000))
                    .signWith(secretKey)
                    .compact();

            assertThrows(JwtAuthenticationException.class,
                    () -> jwtService.parseClaims(token));
        }
    }

    // -------------------------------------------------------------------------
    // validateClaims
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("validateClaims")
    class ValidateClaimsTests {

        @Test
        @DisplayName("Does not throw for a token with a future expiration")
        void futureExpiry_doesNotThrow() {
            String token = Jwts.builder()
                    .subject("1")
                    .expiration(new Date(System.currentTimeMillis() + 60_000))
                    .signWith(secretKey)
                    .compact();

            Claims claims = jwtService.parseClaims(token);

            assertDoesNotThrow(() -> jwtService.validateClaims(claims));
        }

        @Test
        @DisplayName("Throws JwtAuthenticationException when expiration claim is absent")
        void missingExpiry_throwsException() {
            // parseClaims itself will succeed (no expiry = no expiration check at parse time)
            // but validateClaims must detect the missing field
            String token = Jwts.builder()
                    .subject("1")
                    .signWith(secretKey)
                    .compact();

            Claims claims = jwtService.parseClaims(token);

            assertThrows(JwtAuthenticationException.class,
                    () -> jwtService.validateClaims(claims));
        }
    }

    // -------------------------------------------------------------------------
    // generateAccessToken
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("generateAccessToken")
    class GenerateAccessTokenTests {

        @Test
        @DisplayName("Generated token contains the correct user ID as subject")
        void generatedToken_hasCorrectSubject() {
            String token = jwtService.generateAccessToken(99L, "ROLE_USER");

            Claims claims = jwtService.parseClaims(token);

            assertEquals("99", claims.getSubject());
        }

        @Test
        @DisplayName("Generated token contains the correct role claim")
        void generatedToken_hasCorrectRole() {
            String token = jwtService.generateAccessToken(1L, "ROLE_ADMIN");

            Claims claims = jwtService.parseClaims(token);

            assertEquals("ROLE_ADMIN", claims.get("role", String.class));
        }

        @Test
        @DisplayName("Generated token has a non-null expiration")
        void generatedToken_hasExpiration() {
            String token = jwtService.generateAccessToken(1L, "ROLE_USER");

            Claims claims = jwtService.parseClaims(token);

            assertNotNull(claims.getExpiration());
        }

        @Test
        @DisplayName("Generated token expires approximately 15 minutes from now")
        void generatedToken_expiresIn15Minutes() {
            long before = System.currentTimeMillis();
            String token = jwtService.generateAccessToken(1L, "ROLE_USER");
            long after = System.currentTimeMillis();

            Claims claims = jwtService.parseClaims(token);
            long expiryMs = claims.getExpiration().getTime();

            // Allow a 5-second window to account for test execution time
            assertTrue(expiryMs >= before + 14 * 60 * 1000);
            assertTrue(expiryMs <= after + 15 * 60 * 1000 + 5_000);
        }

        @Test
        @DisplayName("Generated token is verifiable by parseClaims (self-consistent round-trip)")
        void generatedToken_roundTrip() {
            String token = jwtService.generateAccessToken(7L, "ROLE_USER");

            assertDoesNotThrow(() -> jwtService.parseClaims(token));
        }
    }

    // -------------------------------------------------------------------------
    // getExpiration
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("getExpiration")
    class GetExpirationTests {

        @Test
        @DisplayName("Returns non-null Date for a token that has an expiration claim")
        void returnsDate_forTokenWithExpiry() {
            String token = jwtService.generateAccessToken(1L, "ROLE_USER");

            Date expiry = jwtService.getExpiration(token);

            assertNotNull(expiry);
        }

        @Test
        @DisplayName("Returned date is in the future for a freshly generated token")
        void returnedDate_isFuture() {
            String token = jwtService.generateAccessToken(1L, "ROLE_USER");

            Date expiry = jwtService.getExpiration(token);

            assertTrue(expiry.after(new Date()));
        }
    }
}