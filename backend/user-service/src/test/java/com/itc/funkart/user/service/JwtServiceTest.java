package com.itc.funkart.user.service;

import com.itc.funkart.user.config.JwtConfig;
import com.itc.funkart.user.dto.user.JwtUserDto;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link JwtService}.
 * Focuses on token lifecycle: Generation, Extraction, and Validation.
 */
class JwtServiceTest {

    private JwtService jwtService;
    private JwtUserDto userDto;

    @BeforeEach
    void setUp() {
        JwtConfig config = new JwtConfig();
        // Min 256-bit secret for HS256
        String secret = Base64.getEncoder().encodeToString("super-secret-key-that-is-long-enough-for-hs256".getBytes());
        config.setSecret(secret);
        config.setExpirationMs(3600000); // 1 hour

        jwtService = new JwtService(config);
        userDto = new JwtUserDto(1L, "Tester", "test@test.com");
    }

    @Test
    @DisplayName("JWT: Should generate and parse a valid token")
    void generateAndParse_Success() {
        String token = jwtService.generateJwtToken(userDto);
        assertNotNull(token);

        Claims claims = jwtService.parseJwtToken(token);
        assertEquals("1", claims.getSubject());
        assertEquals("test@test.com", claims.get("email"));
        assertTrue(jwtService.validateToken(token));
    }

    @Test
    @DisplayName("JWT: Should return false for malformed or invalid token")
    void validateToken_Invalid() {
        assertFalse(jwtService.validateToken("not-a-token"));
    }
}