package com.itc.funkart.gateway.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link JwtWebFilter}.
 *
 * <p>
 * Validates extraction and validation logic for JWT tokens from:
 * <ul>
 *     <li>Authorization header (Bearer token)</li>
 *     <li>HttpOnly cookies</li>
 * </ul>
 * </p>
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
        when(filterChain.filter(any())).thenReturn(Mono.empty());
    }

    @Test
    @DisplayName("Header: Should extract and validate token from Authorization Bearer header")
    void whenValidBearerToken_thenContinueChain() {

        String token = "valid.jwt.token";

        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/test")
                        .header("Authorization", "Bearer " + token)
        );

        Claims claims = Jwts.claims()
                .subject("1")
                .add("name", "Test User")
                .add("email", "test@example.com")
                .build();

        when(jwtTokenValidator.validateAndParseClaims(token)).thenReturn(claims);

        StepVerifier.create(jwtWebFilter.filter(exchange, filterChain))
                .verifyComplete();

        verify(jwtTokenValidator).validateAndParseClaims(token);
        verify(filterChain).filter(exchange);
    }

    @Test
    @DisplayName("Cookie: Should extract and validate token from cookies when header is missing")
    void whenValidCookieToken_thenContinueChain() {

        String token = "cookie.jwt.token";

        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/test")
                        .cookie(new HttpCookie("token", token))
        );

        Claims claims = Jwts.claims()
                .subject("1")
                .add("name", "Test User")
                .add("email", "test@example.com")
                .build();

        when(cookieUtil.extractToken(exchange)).thenReturn(token);
        when(jwtTokenValidator.validateAndParseClaims(token)).thenReturn(claims);
        when(filterChain.filter(any())).thenReturn(Mono.empty());

        StepVerifier.create(jwtWebFilter.filter(exchange, filterChain))
                .verifyComplete();

        verify(cookieUtil).extractToken(exchange);
        verify(jwtTokenValidator).validateAndParseClaims(token);
        verify(filterChain).filter(exchange);
    }

    @Test
    @DisplayName("No Token: Should continue chain without authentication")
    void whenNoToken_thenJustContinueChain() {

        MockServerWebExchange exchange =
                MockServerWebExchange.from(MockServerHttpRequest.get("/api/test"));

        StepVerifier.create(jwtWebFilter.filter(exchange, filterChain))
                .verifyComplete();

        verify(jwtTokenValidator, never()).validateAndParseClaims(anyString());
        verify(filterChain).filter(exchange);
    }
}