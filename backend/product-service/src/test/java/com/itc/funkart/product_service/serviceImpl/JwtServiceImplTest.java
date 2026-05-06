package com.itc.funkart.product_service.serviceImpl;

import com.itc.funkart.product_service.config.JwtConfig;
import com.itc.funkart.product_service.constants.JwtClaims;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;


/**
 * <h2>JwtServiceImplTest</h2>
 * <p>
 * Validates the cryptographic integrity and expiration logic of the JWT service.
 * Tests focus on signature verification and issuer enforcement.
 * </p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("JWT Service Unit Tests")
class JwtServiceImplTest {

    @Mock
    private JwtConfig jwtConfig;
    private JwtServiceImpl jwtService;
    private SecretKey testKey;

    @BeforeEach
    void setUp() {
        // Prepare the key for signing test tokens
        // Use a test secret: "testSecretKeyWithEnoughEntropyForHS256!!" in Base64
        String testSecretBase64 = "dGVzdFNlY3JldEtleVdpdGhFbm91Z2hFbnRyb3B5Rm9ySFMyNTYhIQ==";
        byte[] keyBytes = Base64.getDecoder().decode(testSecretBase64);
        this.testKey = Keys.hmacShaKeyFor(keyBytes);

        // Configure mock and initialize service
        when(jwtConfig.getSecret()).thenReturn(testSecretBase64);
        jwtService = new JwtServiceImpl(jwtConfig);
    }

    // --- Helper to create a test token ---
    private String createTestToken(String issuer, Date expiration) {
        return Jwts.builder()
                .subject("testUser")
                .issuer(issuer)
                .issuedAt(new Date())
                .expiration(expiration)
                .signWith(testKey)
                .compact();
    }

    @Test
    @DisplayName("Parse JWT - Should return claims for valid token")
    void shouldParseValidToken() {
        String token = createTestToken(JwtClaims.ISSUER, new Date(System.currentTimeMillis() + 3600000));

        Claims claims = jwtService.parseJwtToken(token);

        assertThat(claims.getSubject()).isEqualTo("testUser");
        assertThat(claims.getIssuer()).isEqualTo(JwtClaims.ISSUER);
    }

    @Test
    @DisplayName("Parse JWT - Should throw exception for invalid issuer (Branch: Security Check)")
    void shouldThrowExceptionForInvalidIssuer() {
        String token = createTestToken("Malicious-Issuer", new Date(System.currentTimeMillis() + 3600000));

        assertThatThrownBy(() -> jwtService.parseJwtToken(token))
                .isInstanceOf(io.jsonwebtoken.JwtException.class);
    }

    @Test
    @DisplayName("Validate Token - Should return true for active token")
    void shouldValidateActiveToken() {
        String token = createTestToken(JwtClaims.ISSUER, new Date(System.currentTimeMillis() + 3600000));

        boolean isValid = jwtService.validateToken(token);

        assertThat(isValid).isTrue();
    }

    @Test
    @DisplayName("Validate Token - Should return false for expired token (Branch: Temporal Check)")
    void shouldReturnFalseForExpiredToken() {
        // Create token that expired 1 hour ago
        String token = createTestToken(JwtClaims.ISSUER, new Date(System.currentTimeMillis() - 3600000));

        boolean isValid = jwtService.validateToken(token);

        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("Validate Token - Should return false for tampered token (Branch: Cryptographic Integrity)")
    void shouldReturnFalseForTamperedToken() {
        String token = createTestToken(JwtClaims.ISSUER, new Date(System.currentTimeMillis() + 3600000));
        String tamperedToken = token + "modified";

        boolean isValid = jwtService.validateToken(tamperedToken);

        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("Constructor - Should throw exception for invalid Base64 secret")
    void shouldThrowExceptionForInvalidSecret() {
        when(jwtConfig.getSecret()).thenReturn("not-base64-!!!");

        assertThatThrownBy(() -> new JwtServiceImpl(jwtConfig))
                .isInstanceOf(IllegalArgumentException.class);
    }
}