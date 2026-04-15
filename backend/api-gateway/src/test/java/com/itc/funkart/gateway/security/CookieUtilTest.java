package com.itc.funkart.gateway.security;

import com.itc.funkart.gateway.config.AppConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseCookie;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link CookieUtil}.
 * Validates that JWT cookies are correctly attached to and removed from
 * the reactive ServerWebExchange.
 */
class CookieUtilTest {

    private CookieUtil cookieUtil;
    private AppConfig appConfig;

    @BeforeEach
    void setUp() {
        // Mocking the nested configuration structure
        AppConfig.Jwt jwt = new AppConfig.Jwt(
                "YmFzZTY0LXNlY3JldC1rZXktZm9yLXRlc3RpbmctcHVycG9zZXM=", // Dummy B64
                3600000L,
                3600,
                "test-token",
                false
        );

        // Creating the root config (nulls for irrelevant parts of this specific test)
        appConfig = new AppConfig("http://frontend", null, jwt, null, null);
        cookieUtil = new CookieUtil(appConfig);
    }

    @Test
    @DisplayName("Add Cookie: Should attach a valid JWT cookie to the response")
    void whenAddTokenCookie_thenResponseHasCorrectCookie() {
        // Arrange
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/"));
        String dummyJwt = "header.payload.signature";

        // Act
        cookieUtil.addTokenCookie(exchange, dummyJwt, null);

        // Assert
        ResponseCookie cookie = exchange.getResponse().getCookies().getFirst("test-token");

        assertNotNull(cookie, "Cookie should be present in the response");
        assertEquals(dummyJwt, cookie.getValue());
        assertTrue(cookie.isHttpOnly());
        assertEquals("/", cookie.getPath());
        assertEquals(3600, cookie.getMaxAge().getSeconds());
    }

    @Test
    @DisplayName("Clear Cookie: Should set an expired cookie to trigger browser deletion")
    void whenClearTokenCookie_thenCookieIsExpired() {
        // Arrange
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/"));

        // Act
        cookieUtil.clearTokenCookie(exchange);

        // Assert
        ResponseCookie cookie = exchange.getResponse().getCookies().getFirst("test-token");

        assertNotNull(cookie);
        assertEquals("", cookie.getValue());
        assertTrue(cookie.getMaxAge().isZero(), "MaxAge should be 0 to delete the cookie");
    }

    @Test
    @DisplayName("Secure Flag: Should use SameSite=None when secure-cookie is true")
    void whenSecureEnabled_thenSameSiteIsNone() {
        // Arrange: Re-init with secure=true
        AppConfig.Jwt jwtSecure = new AppConfig.Jwt("secret", 3600L, 3600, "token", true);
        AppConfig configSecure = new AppConfig("url", null, jwtSecure, null, null);
        CookieUtil secureUtil = new CookieUtil(configSecure);
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/"));

        // Act
        secureUtil.addTokenCookie(exchange, "jwt", null);

        // Assert
        ResponseCookie cookie = exchange.getResponse().getCookies().getFirst("token");
        assertNotNull(cookie);
        assertEquals("None", cookie.getSameSite());
        assertTrue(cookie.isSecure());
    }
}