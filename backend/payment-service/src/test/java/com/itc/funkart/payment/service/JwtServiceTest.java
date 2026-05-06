package com.itc.funkart.payment.service;

import com.itc.funkart.common.constants.auth.JwtClaims;
import com.itc.funkart.common.dto.user.JwtUserDto;
import com.itc.funkart.payment.config.JwtConfig;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.security.SignatureException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * <h2>JwtServiceTest</h2>
 * <p>
 * Comprehensive test suite for {@link JwtService}.
 * Validates the issuance, parsing, and cryptographic integrity of JSON Web Tokens.
 * </p>
 */
class JwtServiceTest {

    // A valid 256-bit secret for testing
    private static final String PLAIN_SECRET = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";
    private static final String BASE64_SECRET = Base64.getEncoder().encodeToString(PLAIN_SECRET.getBytes());
    private JwtService jwtService;
    private JwtUserDto testUser;

    @BeforeEach
    void setUp() {
        JwtConfig jwtConfig = mock(JwtConfig.class);
        when(jwtConfig.getSecret()).thenReturn(BASE64_SECRET);
        // Matching your service logic: now.plus(jwtConfig.getExpirationMs())
        when(jwtConfig.getExpirationMs()).thenReturn(Duration.ofHours(1));

        jwtService = new JwtService(jwtConfig);

        testUser = JwtUserDto.builder()
                .id(123L)
                .name("Abbas")
                .email("abbas@example.com")
                .role("ROLE_USER")
                .build();
    }

    @Nested
    @DisplayName("Token Issuance & Parsing")
    class LifecycleTests {

        /**
         * Verifies that a generated token can be parsed back into its original claims.
         */
        @Test
        @DisplayName("Should generate and parse a valid token")
        void generateAndParseSuccess() {
            String token = jwtService.generateJwtToken(testUser);
            assertNotNull(token);

            Claims claims = jwtService.parseJwtToken(token);

            assertEquals("123", claims.getSubject());
            assertEquals(JwtClaims.ISSUER, claims.getIssuer());
            assertEquals("Abbas", claims.get(JwtClaims.NAME));
            assertEquals("ROLE_USER", claims.get(JwtClaims.ROLE));
        }

        /**
         * Verifies the boolean validation helper correctly identifies active tokens.
         */
        @Test
        @DisplayName("Should validate an authentic token")
        void validateTokenSuccess() {
            String token = jwtService.generateJwtToken(testUser);
            assertTrue(jwtService.validateToken(token));
        }
    }

    @Nested
    @DisplayName("Security & Integrity")
    class SecurityTests {

        /**
         * Tests the cryptographic signature. If the token string is modified,
         * the parser must throw a SignatureException.
         */
        @Test
        @DisplayName("Should reject tampered tokens")
        void rejectTamperedToken() {
            String token = jwtService.generateJwtToken(testUser);
            // Tamper with the signature part of the JWT
            String tamperedToken = token.substring(0, token.length() - 5) + "abcde";

            assertFalse(jwtService.validateToken(tamperedToken));
            assertThrows(SignatureException.class, () -> jwtService.parseJwtToken(tamperedToken));
        }

        /**
         * Tests temporal validation by mocking a config with a negative expiration.
         */
        @Test
        @DisplayName("Should reject expired tokens")
        void rejectExpiredToken() {
            // 1. Create a "poisoned" mock config
            JwtConfig expiredConfig = mock(JwtConfig.class);
            when(expiredConfig.getSecret()).thenReturn(PLAIN_SECRET);
            when(expiredConfig.getExpirationMs()).thenReturn(Duration.ofHours(-1)); // Expired 1 second ago

            // 2. Inject it into a new service instance
            JwtService expiredService = new JwtService(expiredConfig);

            // 3. Act & Assert
            String token = expiredService.generateJwtToken(testUser);
            assertFalse(expiredService.validateToken(token), "Token should be invalid if expired");
        }

        /**
         * Tests that a token signed with a different secret is rejected.
         */
        @Test
        @DisplayName("Should reject tokens signed with a different key")
        void rejectWrongKey() {
            // Create a token using a different, but VALID Base64 key
            JwtConfig rogueConfig = mock(JwtConfig.class);

            // This is a valid Base64 string, just a different one
            String validBase64Secret = "cm9ndWUtc2VjcmV0LWtleS0xMjM0NTY3ODkwLWFia2xkZg==";

            when(rogueConfig.getSecret()).thenReturn(validBase64Secret);
            when(rogueConfig.getExpirationMs()).thenReturn(Duration.ofHours(1));

            // This constructor will now pass without throwing an IllegalArgumentException
            JwtService rogueService = new JwtService(rogueConfig);
            String rogueToken = rogueService.generateJwtToken(testUser);

            // Your primary jwtService (using the real secret) should now fail to verify it
            assertFalse(jwtService.validateToken(rogueToken));
        }

        /**
         * Verifies that the service enforces the Issuer claim.
         * If a token is presented with a different issuer, it must be rejected.
         */
        @Test
        @DisplayName("Should reject tokens with wrong issuer")
        void rejectWrongIssuer() {
            // Manually build a token with a rogue issuer
            // (Using the same signing key but wrong claims)
            String rogueToken = io.jsonwebtoken.Jwts.builder()
                    .subject("123")
                    .issuer("ROGUE_AUTHORITY")
                    .signWith(io.jsonwebtoken.security.Keys.hmacShaKeyFor(PLAIN_SECRET.getBytes()))
                    .compact();

            assertFalse(jwtService.validateToken(rogueToken));
        }

        @Test
        @DisplayName("Fault: Should throw NPE if secret is null")
        void constructor_NullSecret() {
            JwtConfig config = mock(JwtConfig.class);
            when(config.getSecret()).thenReturn(null);

            assertThrows(NullPointerException.class, () -> new JwtService(config));
        }
    }
}