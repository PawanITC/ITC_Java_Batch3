package com.itc.funkart.user.auth;

import com.itc.funkart.user.config.JwtConfig;
import com.itc.funkart.user.dto.security.UserPrincipalDto;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Base64;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

/**
 * <h2>JwtService — Unit Tests</h2>
 *
 * <p>Covers the full JWT lifecycle owned by the user-service:
 * <ul>
 *   <li>{@code generateJwtToken} — signs a token from a {@link UserPrincipalDto}</li>
 *   <li>{@code parseJwtToken}    — verifies signature and returns raw claims</li>
 *   <li>{@code validateToken}   — boolean guard used by {@link JwtWebFilter}</li>
 *   <li>{@code getUserIdFromToken} — subject extraction shortcut</li>
 * </ul>
 *
 * <p>All tests use a real JJWT key so the cryptographic path is exercised
 * end-to-end. No JWT library internals are mocked.
 *
 * <p><b>Important:</b> The input DTO is {@link UserPrincipalDto} (package
 * {@code dto.security}), NOT any {@code JwtUserDto} — that type does not
 * exist in this service.
 */
class JwtServiceTest {

    /**
     * Base64-encoded secret — decoded value is at least 32 bytes, which
     * satisfies JJWT's minimum key-length requirement for HS256.
     */
    private static final String B64_SECRET = Base64.getEncoder()
            .encodeToString("super-secret-key-that-is-long-enough-for-hs256".getBytes());

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        JwtConfig config = new JwtConfig();
        config.setSecret(B64_SECRET);
        config.setExpirationMs(3_600_000L); // 1 hour
        jwtService = new JwtService(config);
    }

    /** Convenience factory to keep test bodies concise. */
    private UserPrincipalDto principal(Long id, String name, String email, String role) {
        return UserPrincipalDto.builder()
                .userId(id)
                .name(name)
                .email(email)
                .role(role)
                .build();
    }

    // -------------------------------------------------------------------------
    // generateJwtToken
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("generateJwtToken")
    class GenerateTokenTests {

        @Test
        @DisplayName("Returns a non-null, non-blank compact JWT string")
        void returnsNonNullToken() {
            String token = jwtService.generateJwtToken(
                    principal(1L, "Alice", "alice@example.com", "ROLE_USER"));
            assertNotNull(token);
            assertFalse(token.isBlank());
        }

        @Test
        @DisplayName("Subject is the user ID serialised as a string")
        void subjectIsUserId() {
            String token = jwtService.generateJwtToken(
                    principal(42L, "Alice", "alice@example.com", "ROLE_USER"));
            assertEquals("42", jwtService.parseJwtToken(token).getSubject());
        }

        @Test
        @DisplayName("name claim matches the principal's name")
        void containsNameClaim() {
            String token = jwtService.generateJwtToken(
                    principal(1L, "Alice", "alice@example.com", "ROLE_USER"));
            assertEquals("Alice", jwtService.parseJwtToken(token).get("name", String.class));
        }

        @Test
        @DisplayName("email claim matches the principal's email")
        void containsEmailClaim() {
            String token = jwtService.generateJwtToken(
                    principal(1L, "Alice", "alice@example.com", "ROLE_USER"));
            assertEquals("alice@example.com",
                    jwtService.parseJwtToken(token).get("email", String.class));
        }

        @Test
        @DisplayName("role claim matches the principal's role")
        void containsRoleClaim() {
            String token = jwtService.generateJwtToken(
                    principal(1L, "Alice", "alice@example.com", "ROLE_ADMIN"));
            assertEquals("ROLE_ADMIN",
                    jwtService.parseJwtToken(token).get("role", String.class));
        }

        @Test
        @DisplayName("Expiration date is in the future")
        void hasFutureExpiration() {
            String token = jwtService.generateJwtToken(
                    principal(1L, "Alice", "alice@example.com", "ROLE_USER"));
            Claims claims = jwtService.parseJwtToken(token);
            assertNotNull(claims.getExpiration());
            assertTrue(claims.getExpiration().after(new Date()));
        }

        @Test
        @DisplayName("Issuer is funkart-user-service")
        void issuerIsCorrect() {
            String token = jwtService.generateJwtToken(
                    principal(1L, "Alice", "alice@example.com", "ROLE_USER"));
            assertEquals("funkart-user-service",
                    jwtService.parseJwtToken(token).getIssuer());
        }
    }

    // -------------------------------------------------------------------------
    // parseJwtToken
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("parseJwtToken")
    class ParseTokenTests {

        @Test
        @DisplayName("Parses own generated token without throwing")
        void parsesOwnToken() {
            String token = jwtService.generateJwtToken(
                    principal(1L, "Alice", "alice@example.com", "ROLE_USER"));
            assertDoesNotThrow(() -> jwtService.parseJwtToken(token));
        }

        @Test
        @DisplayName("Throws for a completely invalid string")
        void throwsOnGarbage() {
            assertThrows(Exception.class, () -> jwtService.parseJwtToken("not.a.jwt"));
        }

        @Test
        @DisplayName("Throws when signature is tampered with")
        void throwsOnTamperedSignature() {
            String token = jwtService.generateJwtToken(
                    principal(1L, "Alice", "alice@example.com", "ROLE_USER"));
            assertThrows(Exception.class, () -> jwtService.parseJwtToken(token + "x"));
        }
    }

    // -------------------------------------------------------------------------
    // validateToken
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("validateToken")
    class ValidateTokenTests {

        @Test
        @DisplayName("Returns true for a freshly generated token")
        void trueForValidToken() {
            String token = jwtService.generateJwtToken(
                    principal(1L, "Alice", "alice@example.com", "ROLE_USER"));
            assertTrue(jwtService.validateToken(token));
        }

        @Test
        @DisplayName("Returns false for a malformed string")
        void falseForMalformed() {
            assertFalse(jwtService.validateToken("garbage"));
        }

        @Test
        @DisplayName("Returns false for a blank string")
        void falseForBlank() {
            assertFalse(jwtService.validateToken(""));
        }

        @Test
        @DisplayName("Returns false for a tampered token")
        void falseForTampered() {
            String token = jwtService.generateJwtToken(
                    principal(1L, "Alice", "alice@example.com", "ROLE_USER"));
            assertFalse(jwtService.validateToken(token + "tampered"));
        }
    }

    // -------------------------------------------------------------------------
    // getUserIdFromToken
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("getUserIdFromToken")
    class GetUserIdTests {

        @Test
        @DisplayName("Extracts correct user ID from a valid token")
        void extractsCorrectId() {
            String token = jwtService.generateJwtToken(
                    principal(99L, "Alice", "alice@example.com", "ROLE_USER"));
            assertEquals(99L, jwtService.getUserIdFromToken(token));
        }

        @Test
        @DisplayName("Throws when token is invalid")
        void throwsOnInvalidToken() {
            assertThrows(Exception.class, () -> jwtService.getUserIdFromToken("bad.token"));
        }
    }
}