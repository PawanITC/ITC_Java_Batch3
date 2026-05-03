package com.itc.funkart.product.serviceImpl;

import com.itc.funkart.common.constants.auth.JwtClaims;
import com.itc.funkart.product.config.JwtConfig;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.IncorrectClaimException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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
 * Validates the cryptographic integrity and temporal validity of JWTs.
 * Ensures that the service adheres to the security contract defined by FunKart Auth.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("JWT Service - Behavioral Analysis")
class JwtServiceImplTest {

    @Mock
    private JwtConfig jwtConfig;
    private JwtServiceImpl jwtService;

    @Nested
    @DisplayName("Initialization Logic")
    class Initialization {
        /**
         * Verifies that the service fails fast if the configuration is corrupted.
         */
        @Test
        @DisplayName("Should fail when secret is not valid Base64")
        void shouldFailOnInvalidBase64() {
            when(jwtConfig.getSecret()).thenReturn("!!!-Not-Base64-!!!");
            assertThatThrownBy(() -> new JwtServiceImpl(jwtConfig))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("Token Parsing & Validation")
    class Parsing {
        private SecretKey testKey;

        @BeforeEach
        void setUp() {
            String VALID_SECRET = "dGVzdFNlY3JldEtleVdpdGhFbm91Z2hFbnRyb3B5Rm9ySFMyNTYhIQ==";
            when(jwtConfig.getSecret()).thenReturn(VALID_SECRET);
            testKey = Keys.hmacShaKeyFor(Base64.getDecoder().decode(VALID_SECRET));
            jwtService = new JwtServiceImpl(jwtConfig);
        }

        /**
         * Helper to generate test tokens with specific properties.
         */
        private String generateToken(String issuer, Date exp) {
            return Jwts.builder()
                    .subject("12345")
                    .issuer(issuer)
                    .expiration(exp)
                    .signWith(testKey)
                    .compact();
        }

        @Test
        @DisplayName("Success: Should extract claims from a perfectly valid token")
        void shouldReturnClaims() {
            String token = generateToken(JwtClaims.ISSUER, new Date(System.currentTimeMillis() + 60000));
            Claims claims = jwtService.parseJwtToken(token);
            assertThat(claims.getSubject()).isEqualTo("12345");
        }

        @Test
        @DisplayName("Failure: Should reject tokens with an expired 'exp' claim")
        void shouldThrowOnExpired() {
            String token = generateToken(JwtClaims.ISSUER, new Date(System.currentTimeMillis() - 1000));
            assertThatThrownBy(() -> jwtService.parseJwtToken(token))
                    .isInstanceOf(ExpiredJwtException.class);
        }

        @Test
        @DisplayName("Failure: Should reject tokens if the cryptographic signature is altered")
        void shouldThrowOnTampering() {
            String token = generateToken(JwtClaims.ISSUER, new Date(System.currentTimeMillis() + 60000));
            assertThatThrownBy(() -> jwtService.parseJwtToken(token + "tampered"))
                    .isInstanceOf(SignatureException.class);
        }

        @Test
        @DisplayName("Failure: Should reject tokens issued by an untrusted authority")
        void shouldThrowOnInvalidIssuer() {
            // We generate a token with a 'bad' issuer
            String token = generateToken("Untrusted-Source", new Date(System.currentTimeMillis() + 60000));

            assertThatThrownBy(() -> jwtService.parseJwtToken(token))
                    .isInstanceOf(IncorrectClaimException.class)
                    // Change "issuer" to "iss" to match JJWT's internal error message
                    .hasMessageContaining("iss")
                    .hasMessageContaining("Untrusted-Source");
        }
    }
}