package com.itc.funkart.gateway.security;

import com.itc.funkart.gateway.config.AppConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseCookie;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.test.StepVerifier;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * <h2>CookieUtil — Unit Tests</h2>
 *
 * <p>Validates all cookie lifecycle operations: issuing access-token cookies,
 * issuing refresh-token cookies, extracting tokens from requests, and clearing
 * sessions on logout.
 *
 * <p>These tests use {@link MockServerWebExchange} to inspect the mutated
 * response headers without spinning up a real server.
 *
 * <p><b>Cookie contract summary:</b>
 * <table border="1">
 *   <tr><th>Cookie</th><th>HttpOnly</th><th>Path</th><th>SameSite (non-secure)</th><th>Max-Age</th></tr>
 *   <tr><td>access token</td><td>true</td><td>/</td><td>Lax</td><td>configurable</td></tr>
 *   <tr><td>refresh_token</td><td>true</td><td>/</td><td>Strict</td><td>7 days</td></tr>
 *   <tr><td>clear (logout)</td><td>true</td><td>/</td><td>Lax</td><td>0</td></tr>
 * </table>
 */
class CookieUtilTest {

    private CookieUtil cookieUtil;
    private AppConfig.Jwt jwtConfig;

    /**
     * Wires a fresh {@link CookieUtil} before every test using a mocked
     * {@link AppConfig} so we can control cookie name, max-age, and secure flag.
     */
    @BeforeEach
    void setUp() {
        AppConfig appConfig = mock(AppConfig.class);
        jwtConfig = mock(AppConfig.Jwt.class);

        when(appConfig.jwt()).thenReturn(jwtConfig);
        when(jwtConfig.cookieName()).thenReturn("test-token");
        when(jwtConfig.cookieMaxAgeSeconds()).thenReturn(3600);
        when(jwtConfig.secureCookie()).thenReturn(false);

        cookieUtil = new CookieUtil(appConfig);
    }

    // -------------------------------------------------------------------------
    // addTokenCookie
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("addTokenCookie")
    class AddTokenCookieTests {

        @Test
        @DisplayName("Mono completes without error")
        void mono_completesCleanly() {
            var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/"));

            StepVerifier.create(cookieUtil.addTokenCookie(exchange, "jwt-token"))
                    .verifyComplete();
        }

        @Test
        @DisplayName("Cookie value matches supplied token")
        void cookieValue_matchesToken() {
            var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/"));

            cookieUtil.addTokenCookie(exchange, "my-jwt").block();

            ResponseCookie cookie = exchange.getResponse().getCookies().getFirst("test-token");
            assertNotNull(cookie);
            assertEquals("my-jwt", cookie.getValue());
        }

        @Test
        @DisplayName("Cookie is HttpOnly")
        void cookie_isHttpOnly() {
            var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/"));

            cookieUtil.addTokenCookie(exchange, "tok").block();

            ResponseCookie cookie = exchange.getResponse().getCookies().getFirst("test-token");
            assertNotNull(cookie);
            assertTrue(cookie.isHttpOnly());
        }

        @Test
        @DisplayName("Cookie path is /")
        void cookie_pathIsRoot() {
            var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/"));

            cookieUtil.addTokenCookie(exchange, "tok").block();

            ResponseCookie cookie = exchange.getResponse().getCookies().getFirst("test-token");
            assertNotNull(cookie);
            assertEquals("/", cookie.getPath());
        }

        @Test
        @DisplayName("SameSite is Lax when secure=false")
        void sameSite_laxWhenNotSecure() {
            var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/"));

            cookieUtil.addTokenCookie(exchange, "tok").block();

            ResponseCookie cookie = exchange.getResponse().getCookies().getFirst("test-token");
            assertNotNull(cookie);
            assertEquals("Lax", cookie.getSameSite());
        }

        @Test
        @DisplayName("SameSite is None and Secure flag set when secure=true")
        void sameSite_noneAndSecureWhenSecureEnabled() {
            when(jwtConfig.secureCookie()).thenReturn(true);
            var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/"));

            cookieUtil.addTokenCookie(exchange, "tok").block();

            ResponseCookie cookie = exchange.getResponse().getCookies().getFirst("test-token");
            assertNotNull(cookie);
            assertEquals("None", cookie.getSameSite());
            assertTrue(cookie.isSecure());
        }

        @Test
        @DisplayName("Max-age comes from JWT config")
        void maxAge_matchesConfig() {
            when(jwtConfig.cookieMaxAgeSeconds()).thenReturn(7200);
            var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/"));

            cookieUtil.addTokenCookie(exchange, "tok").block();

            ResponseCookie cookie = exchange.getResponse().getCookies().getFirst("test-token");
            assertNotNull(cookie);
            assertEquals(Duration.ofSeconds(7200), cookie.getMaxAge());
        }
    }

    // -------------------------------------------------------------------------
    // clearTokenCookie
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("clearTokenCookie")
    class ClearTokenCookieTests {

        @Test
        @DisplayName("Mono completes without error")
        void mono_completesCleanly() {
            var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/"));

            StepVerifier.create(cookieUtil.clearTokenCookie(exchange))
                    .verifyComplete();
        }

        @Test
        @DisplayName("Cookie value is blank (invalidates the session)")
        void cookieValue_isBlank() {
            var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/"));

            cookieUtil.clearTokenCookie(exchange).block();

            ResponseCookie cookie = exchange.getResponse().getCookies().getFirst("test-token");
            assertNotNull(cookie);
            assertEquals("", cookie.getValue());
        }

        @Test
        @DisplayName("Max-age is zero (instructs browser to delete cookie)")
        void maxAge_isZero() {
            var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/"));

            cookieUtil.clearTokenCookie(exchange).block();

            ResponseCookie cookie = exchange.getResponse().getCookies().getFirst("test-token");
            assertNotNull(cookie);
            assertTrue(cookie.getMaxAge().isZero());
        }

        @Test
        @DisplayName("Cookie is still HttpOnly after clearing")
        void cookie_remainsHttpOnly() {
            var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/"));

            cookieUtil.clearTokenCookie(exchange).block();

            ResponseCookie cookie = exchange.getResponse().getCookies().getFirst("test-token");
            assertNotNull(cookie);
            assertTrue(cookie.isHttpOnly());
        }
    }

    // -------------------------------------------------------------------------
    // addRefreshCookie
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("addRefreshCookie")
    class AddRefreshCookieTests {

        @Test
        @DisplayName("Mono completes without error")
        void mono_completesCleanly() {
            var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/"));

            StepVerifier.create(cookieUtil.addRefreshCookie(exchange, "refresh-tok"))
                    .verifyComplete();
        }

        @Test
        @DisplayName("Cookie name is refresh_token")
        void cookieName_isRefreshToken() {
            var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/"));

            cookieUtil.addRefreshCookie(exchange, "refresh-tok").block();

            ResponseCookie cookie = exchange.getResponse().getCookies().getFirst("refresh_token");
            assertNotNull(cookie, "Expected cookie named 'refresh_token'");
        }

        @Test
        @DisplayName("Cookie value matches supplied refresh token")
        void cookieValue_matchesToken() {
            var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/"));

            cookieUtil.addRefreshCookie(exchange, "my-refresh").block();

            ResponseCookie cookie = exchange.getResponse().getCookies().getFirst("refresh_token");
            assertNotNull(cookie);
            assertEquals("my-refresh", cookie.getValue());
        }

        @Test
        @DisplayName("Max-age is 7 days")
        void maxAge_isSevenDays() {
            var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/"));

            cookieUtil.addRefreshCookie(exchange, "tok").block();

            ResponseCookie cookie = exchange.getResponse().getCookies().getFirst("refresh_token");
            assertNotNull(cookie);
            assertEquals(Duration.ofDays(7), cookie.getMaxAge());
        }

        @Test
        @DisplayName("SameSite is Strict (tighter than access-token cookie)")
        void sameSite_isStrict() {
            var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/"));

            cookieUtil.addRefreshCookie(exchange, "tok").block();

            ResponseCookie cookie = exchange.getResponse().getCookies().getFirst("refresh_token");
            assertNotNull(cookie);
            assertEquals("Strict", cookie.getSameSite());
        }

        @Test
        @DisplayName("Cookie is HttpOnly")
        void cookie_isHttpOnly() {
            var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/"));

            cookieUtil.addRefreshCookie(exchange, "tok").block();

            ResponseCookie cookie = exchange.getResponse().getCookies().getFirst("refresh_token");
            assertNotNull(cookie);
            assertTrue(cookie.isHttpOnly());
        }
    }

    // -------------------------------------------------------------------------
    // extractToken
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("extractToken")
    class ExtractTokenTests {

        @Test
        @DisplayName("Extracts token from HttpOnly cookie by configured name")
        void extractsFromCookie() {
            var exchange = MockServerWebExchange.from(
                    MockServerHttpRequest.get("/")
                            .cookie(new org.springframework.http.HttpCookie("test-token", "cookie-jwt"))
            );

            String token = cookieUtil.extractToken(exchange);

            assertEquals("cookie-jwt", token);
        }

        @Test
        @DisplayName("Falls back to Authorization Bearer header when no cookie present")
        void fallsBackToAuthHeader() {
            var exchange = MockServerWebExchange.from(
                    MockServerHttpRequest.get("/")
                            .header("Authorization", "Bearer header-jwt")
            );

            String token = cookieUtil.extractToken(exchange);

            assertEquals("header-jwt", token);
        }

        @Test
        @DisplayName("Returns null when neither cookie nor Authorization header is present")
        void returnsNullWhenAbsent() {
            var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/"));

            String token = cookieUtil.extractToken(exchange);

            assertNull(token);
        }

        @Test
        @DisplayName("Cookie takes precedence over Authorization header")
        void cookiePrecedenceOverHeader() {
            var exchange = MockServerWebExchange.from(
                    MockServerHttpRequest.get("/")
                            .cookie(new org.springframework.http.HttpCookie("test-token", "cookie-jwt"))
                            .header("Authorization", "Bearer header-jwt")
            );

            String token = cookieUtil.extractToken(exchange);

            // Cookie wins
            assertEquals("cookie-jwt", token);
        }
    }
}