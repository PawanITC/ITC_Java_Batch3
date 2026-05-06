package com.itc.funkart.config;

import com.itc.funkart.common.constants.auth.JwtClaims;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * <h2>JwtServiceTest</h2>
 * <p>
 * Validates the cryptographic operations of the {@link JwtService}.
 * This test ensures that the JVM can correctly decode secrets and verify signatures.
 * </p>
 */
@ExtendWith(MockitoExtension.class)
class JwtServiceTest {

    // A valid Base64 encoded 256-bit string for testing
    private final String MOCK_SECRET = Base64.getEncoder().encodeToString("super-secret-key-that-is-at-least-32-bytes-long".getBytes());
    @Mock
    private JwtConfig jwtConfig;
    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        // Arrange: Mock the config to return our test secret
        when(jwtConfig.getSecret()).thenReturn(MOCK_SECRET);

        // Act: Initialize the service (This tests the constructor logic)
        jwtService = new JwtService(jwtConfig);
    }

    /**
     * <h3>Test: Successful JWT Parsing</h3>
     * <p>
     * Verifies that a token signed with the same secret and correct issuer
     * is successfully parsed into its claims.
     * </p>
     */
    @Test
    @DisplayName("parseJwtToken - Success with valid token")
    void parseJwtToken_shouldReturnClaims_whenTokenIsValid() {
        // Arrange: Create a valid token manually for testing
        byte[] keyBytes = Base64.getDecoder().decode(MOCK_SECRET);
        SecretKey key = Keys.hmacShaKeyFor(keyBytes);

        String token = Jwts.builder()
                .subject("test-user")
                .issuer(JwtClaims.ISSUER)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 3600000))
                .signWith(key)
                .compact();

        // Act
        Claims claims = jwtService.parseJwtToken(token);

        // Assert
        assertNotNull(claims);
        assertEquals("test-user", claims.getSubject());
        assertEquals(JwtClaims.ISSUER, claims.getIssuer());
    }

    /**
     * <h3>Test: Signature Mismatch</h3>
     * <p>
     * Verifies that the service throws an exception if the token signature
     * was created with a different key, preventing unauthorized access.
     * </p>
     */
    @Test
    @DisplayName("parseJwtToken - Throw Exception on Invalid Signature")
    void parseJwtToken_shouldThrow_whenSignatureIsInvalid() {
        // Arrange: Create a token with a DIFFERENT key
        SecretKey wrongKey = Keys.hmacShaKeyFor("wrong-secret-key-wrong-secret-key-wrong-secret-key".getBytes());
        String invalidToken = Jwts.builder()
                .subject("hacker")
                .signWith(wrongKey)
                .compact();

        // Act & Assert
        assertThrows(Exception.class, () -> jwtService.parseJwtToken(invalidToken));
    }

    /**
     * <h3>Test: Constructor Error Handling</h3>
     * <p>
     * Ensures that if the secret in the configuration is not valid Base64,
     * the JVM throws an {@link IllegalArgumentException}.
     * </p>
     */
    @Test
    @DisplayName("Constructor - Fail on Invalid Base64 Secret")
    void constructor_shouldThrow_whenSecretIsNotBase64() {
        // Arrange: Use a string that is invalid Base64 (e.g., contains '@')
        when(jwtConfig.getSecret()).thenReturn("invalid-base64-secret-@#$");

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> new JwtService(jwtConfig));
    }
}