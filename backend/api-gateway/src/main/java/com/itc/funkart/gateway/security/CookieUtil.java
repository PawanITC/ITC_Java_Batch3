package com.itc.funkart.gateway.security;

import com.itc.funkart.gateway.config.JwtCookieConfig;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import java.time.Duration;

/**
 * <h2>Cookie Management Utility</h2>
 * <p>Handles the lifecycle of the JWT Session Cookie. By using <b>HttpOnly</b> cookies,
 * we protect the JWT from being stolen by malicious JavaScript (XSS attacks).</p>
 */
@Component
public class CookieUtil {

    private final JwtCookieConfig config;

    public CookieUtil(JwtCookieConfig config) {
        this.config = config;
    }

    /**
     * Issues a secure JWT cookie.
     * <p>Note: <b>SameSite=None</b> is required for cross-site requests (e.g., Frontend on Vercel
     * talking to Backend on AWS), but it <i>must</i> be paired with the <b>Secure</b> flag.</p>
     */
    public void addTokenCookie(ServerWebExchange exchange, String token, Integer maxAgeSeconds) {

        int age = (maxAgeSeconds != null)
                ? maxAgeSeconds
                : config.cookieMaxAgeSeconds();

        ResponseCookie cookie = ResponseCookie
                .from(config.cookieName(), token)
                .httpOnly(true)
                .secure(config.secureCookie())
                .path("/")
                .maxAge(Duration.ofSeconds(age))
                .sameSite(config.secureCookie() ? "None" : "Lax")
                .build();

        exchange.getResponse().addCookie(cookie);
    }

    /**
     * Method to extract token.
     */
    public String extractToken(ServerWebExchange exchange) {
        var cookie = exchange.getRequest()
                .getCookies()
                .getFirst(config.cookieName());

        return (cookie != null) ? cookie.getValue() : null;
    }

    /**
     * Instructs the browser to delete the session cookie immediately.
     */
    public void clearTokenCookie(ServerWebExchange exchange) {

        ResponseCookie cookie = ResponseCookie
                .from(config.cookieName(), "")
                .httpOnly(true)
                .secure(config.secureCookie())
                .path("/")
                .maxAge(Duration.ZERO)
                .sameSite(config.secureCookie() ? "None" : "Lax")
                .build();

        exchange.getResponse().addCookie(cookie);
    }
}