package com.itc.funkart.gateway.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts; // Use this for modern claims creation
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpCookie;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link JwtWebFilter}.
 * Validates the extraction and validation logic for JWT tokens from both
 * HTTP Headers and Browser Cookies.
 */
@ExtendWith(MockitoExtension.class)
class JwtWebFilterTest {

    @Mock
    private CookieUtil cookieUtil;

    @Mock
    private JwtTokenValidator jwtTokenValidator;

    @Mock
    private WebFilterChain filterChain;

    private JwtWebFilter jwtWebFilter;

    @BeforeEach
    void setUp() {
        jwtWebFilter = new JwtWebFilter(cookieUtil, jwtTokenValidator);
        // Ensure the filter chain always continues
        when(filterChain.filter(any())).thenReturn(Mono.empty());
    }

    /**
     * Verifies that a valid JWT in the Authorization header is correctly identified
     * and processed. Using Jwts.claims() for modern JJWT compatibility.
     */
    @Test
    @DisplayName("Header: Should extract and validate token from Authorization Bearer header")
    void whenValidBearerToken_thenContinueChain() {
        // Arrange
        String token = "valid.jwt.token";
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/test")
                        .header("Authorization", "Bearer " + token)
        );

        // Modern way to create claims for testing
        Claims claims = Jwts.claims()
                .subject("1")
                .add("name", "Test User")
                .add("email", "test@example.com")
                .build();

        when(jwtTokenValidator.validateAndParseClaims(token)).thenReturn(claims);

        // Act & Assert
        StepVerifier.create(jwtWebFilter.filter(exchange, filterChain))
                .verifyComplete();

        verify(jwtTokenValidator).validateAndParseClaims(token);
        verify(filterChain).filter(exchange);
    }

    @Test
    @DisplayName("Cookie: Should extract and validate token from cookies when header is missing")
    void whenValidCookieToken_thenContinueChain() {
        // Arrange
        String token = "cookie.jwt.token";
        String cookieName = "token";

        when(cookieUtil.getCookieName()).thenReturn(cookieName);

        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/test")
                        .cookie(new HttpCookie(cookieName, token))
        );

        Claims claims = Jwts.claims()
                .subject("1")
                .add("name", "Test User")
                .add("email", "test@example.com")
                .build();

        when(jwtTokenValidator.validateAndParseClaims(token)).thenReturn(claims);

        // Act & Assert
        StepVerifier.create(jwtWebFilter.filter(exchange, filterChain))
                .verifyComplete();

        verify(jwtTokenValidator).validateAndParseClaims(token);
    }

    @Test
    @DisplayName("No Token: Should simply continue the chain if no token is found")
    void whenNoToken_thenJustContinueChain() {
        // Arrange
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/test"));
        when(cookieUtil.getCookieName()).thenReturn("token");

        // Act & Assert
        StepVerifier.create(jwtWebFilter.filter(exchange, filterChain))
                .verifyComplete();

        verify(jwtTokenValidator, never()).validateAndParseClaims(anyString());
        verify(filterChain).filter(exchange);
    }
}