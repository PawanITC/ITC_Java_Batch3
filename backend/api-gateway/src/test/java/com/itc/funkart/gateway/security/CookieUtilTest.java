package com.itc.funkart.gateway.security;

import com.itc.funkart.gateway.config.JwtCookieConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.http.ResponseCookie;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link CookieUtil}.
 *
 * <p>
 * Ensures JWT cookies are correctly created and cleared using reactive response handling.
 * This test does not load Spring context and validates pure business logic.
 * </p>
 */
class CookieUtilTest {

    private CookieUtil cookieUtil;

    @BeforeEach
    void setUp() {

        JwtCookieConfig config = new JwtCookieConfig(
                "test-token",
                false,
                3600
        );

        cookieUtil = new CookieUtil(config);
    }

    @Test
    @DisplayName("Should attach JWT cookie correctly")
    void addCookie_success() {

        var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/"));

        cookieUtil.addTokenCookie(exchange, "jwt-token", null);

        ResponseCookie cookie =
                exchange.getResponse().getCookies().getFirst("test-token");

        assertNotNull(cookie);
        assertEquals("jwt-token", cookie.getValue());
        assertTrue(cookie.isHttpOnly());
        assertEquals("/", cookie.getPath());
        assertEquals(3600, cookie.getMaxAge().getSeconds());
    }

    @Test
    @DisplayName("Should clear cookie with zero max age")
    void clearCookie_success() {

        var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/"));

        cookieUtil.clearTokenCookie(exchange);

        ResponseCookie cookie =
                exchange.getResponse().getCookies().getFirst("test-token");

        assertNotNull(cookie);
        assertEquals("", cookie.getValue());
        assertTrue(cookie.getMaxAge().isZero());
    }

    @Test
    @DisplayName("Should set SameSite=None when secure enabled")
    void secureCookie_setsSameSiteNone() {

        JwtCookieConfig secureConfig = new JwtCookieConfig(
                "token",
                true,
                3600
        );

        CookieUtil secureUtil = new CookieUtil(secureConfig);

        var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/"));

        secureUtil.addTokenCookie(exchange, "jwt", null);

        ResponseCookie cookie =
                exchange.getResponse().getCookies().getFirst("token");

        assertNotNull(cookie);
        assertEquals("None", cookie.getSameSite());
        assertTrue(cookie.isSecure());
    }
}