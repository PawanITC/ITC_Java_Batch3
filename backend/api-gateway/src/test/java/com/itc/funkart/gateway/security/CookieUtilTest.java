package com.itc.funkart.gateway.security;

import com.itc.funkart.gateway.config.AppConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseCookie;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * <h2>Cookie Utility Unit Tests</h2>
 * <p>
 * Validates the reactive lifecycle of session cookies. Ensures that headers are
 * correctly mutated within the WebFlux exchange.
 * </p>
 */
class CookieUtilTest {

    private CookieUtil cookieUtil;
    private AppConfig.Jwt jwtConfig;

    @BeforeEach
    void setUp() {
        AppConfig appConfig = mock(AppConfig.class);
        jwtConfig = mock(AppConfig.Jwt.class);

        // Standard mock configuration
        when(appConfig.jwt()).thenReturn(jwtConfig);
        when(jwtConfig.cookieName()).thenReturn("test-token");
        when(jwtConfig.cookieMaxAgeSeconds()).thenReturn(3600);
        when(jwtConfig.secureCookie()).thenReturn(false);

        cookieUtil = new CookieUtil(appConfig);
    }

    @Test
    @DisplayName("addTokenCookie: should correctly populate ResponseCookie in exchange")
    void addCookie_success() {
        var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/"));

        // Use StepVerifier to execute the Mono
        StepVerifier.create(cookieUtil.addTokenCookie(exchange, "jwt-token"))
                .verifyComplete();

        ResponseCookie cookie = exchange.getResponse().getCookies().getFirst("test-token");

        assertNotNull(cookie);
        assertEquals("jwt-token", cookie.getValue());
        assertTrue(cookie.isHttpOnly());
        assertEquals("/", cookie.getPath());
        assertEquals("Lax", cookie.getSameSite()); // Secure is false, should be Lax
    }

    @Test
    @DisplayName("clearTokenCookie: should invalidate cookie with zero max-age")
    void clearCookie_success() {
        var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/"));

        StepVerifier.create(cookieUtil.clearTokenCookie(exchange))
                .verifyComplete();

        ResponseCookie cookie = exchange.getResponse().getCookies().getFirst("test-token");

        assertNotNull(cookie);
        assertEquals("", cookie.getValue());
        assertTrue(cookie.getMaxAge().isZero());
    }

    @Test
    @DisplayName("SameSite: should set None and Secure when config is enabled")
    void secureCookie_setsSameSiteNone() {
        // Toggle secure config for this specific test
        when(jwtConfig.secureCookie()).thenReturn(true);

        var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/"));

        StepVerifier.create(cookieUtil.addTokenCookie(exchange, "jwt"))
                .verifyComplete();

        ResponseCookie cookie = exchange.getResponse().getCookies().getFirst("test-token");

        assertNotNull(cookie);
        assertEquals("None", cookie.getSameSite());
        assertTrue(cookie.isSecure());
    }
}