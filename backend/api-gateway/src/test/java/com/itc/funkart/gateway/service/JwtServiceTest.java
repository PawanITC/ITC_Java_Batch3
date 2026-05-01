package com.itc.funkart.gateway.service;

import com.itc.funkart.gateway.auth.jwt.JwtClaims;
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
import java.time.Duration;
import java.util.Date;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private static final String SECRET = "YmFzZTY0LWVuY29kZWQtc2VjcmV0LWtleS1tdXN0LWJlLTMyLWJ5dGVzLWxvbmc=";
    private JwtService jwtService;
    private SecretKey secretKey;

    @BeforeEach
    void setUp() {
        AppConfig.Jwt jwtConfig = new AppConfig.Jwt(SECRET, Duration.ofMillis(3600000), 3600, "token", false);
        AppConfig appConfig = new AppConfig("http://localhost:5173", "/oauth", jwtConfig, null, Map.of());
        jwtService = new JwtService(appConfig);
        byte[] keyBytes = java.util.Base64.getDecoder().decode(SECRET);
        secretKey = Keys.hmacShaKeyFor(keyBytes);
    }

    @Nested
    @DisplayName("Cryptographic & Expiration Tests")
    class SecurityTests {
        @Test
        @DisplayName("Should throw for expired token beyond 30s skew")
        void expiredBeyondSkew() {
            String token = Jwts.builder()
                    .subject("1").issuer(JwtClaims.ISSUER)
                    .expiration(new Date(System.currentTimeMillis() - 40000))
                    .signWith(secretKey).compact();
            assertThrows(JwtAuthenticationException.class, () -> jwtService.parseClaims(token));
        }

        @Test
        @DisplayName("Should accept token within 30s clock skew")
        void withinSkew() {
            String token = Jwts.builder()
                    .subject("1").issuer(JwtClaims.ISSUER)
                    .expiration(new Date(System.currentTimeMillis() - 10000))
                    .signWith(secretKey).compact();
            assertDoesNotThrow(() -> jwtService.parseClaims(token));
        }
    }

    @Nested
    @DisplayName("Data Extraction Tests")
    class ExtractionTests {
        @Test
        @DisplayName("Should extract valid Long ID")
        void validId() {
            Claims claims = Jwts.claims().subject("99").build();
            assertEquals(99L, jwtService.getUserId(claims));
        }

        @Test
        @DisplayName("Should throw for malformed subject")
        void malformedId() {
            Claims claims = Jwts.claims().subject("user_one").build();
            assertThrows(JwtAuthenticationException.class, () -> jwtService.getUserId(claims));
        }

        @Test
        @DisplayName("Should extract expiration date")
        void validExpiry() {
            // 1. Create a date and strip milliseconds for consistency
            long nowMillis = (System.currentTimeMillis() / 1000) * 1000;
            Date future = new Date(nowMillis + 100000);

            Claims claims = Jwts.claims().expiration(future).build();

            // 2. Compare based on seconds to avoid precision mismatch (or instead of comparing if less or within 1000ms (1 second) range
            assertEquals(future.getTime() / 1000, jwtService.getExpiration(claims).getTime() / 1000);
        }
    }

    @Nested
    @DisplayName("Token Generation (Round-Trip)")
    class RoundTripTests {
        @Test
        @DisplayName("Generated token should be fully parseable")
        void generateAndParse() {
            String token = jwtService.generateAccessToken(123L, "ROLE_ADMIN");
            Claims claims = jwtService.parseClaims(token);
            assertEquals(123L, jwtService.getUserId(claims));
            assertEquals("ROLE_ADMIN", claims.get("role", String.class));
            assertNotNull(jwtService.getExpiration(claims));
        }
    }
}