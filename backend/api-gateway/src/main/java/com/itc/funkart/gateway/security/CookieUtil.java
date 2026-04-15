package com.itc.funkart.gateway.security;

import com.itc.funkart.gateway.config.AppConfig;
import lombok.Getter;
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

    @Getter
    private final String cookieName;
    private final boolean secure;
    private final int defaultMaxAgeSeconds;

    public CookieUtil(AppConfig appConfig) {
        this.cookieName = appConfig.jwt().cookieName();
        this.secure = appConfig.jwt().secureCookie();
        this.defaultMaxAgeSeconds = appConfig.jwt().cookieMaxAgeSeconds();
    }

    /**
     * Issues a secure JWT cookie.
     * <p>Note: <b>SameSite=None</b> is required for cross-site requests (e.g., Frontend on Vercel
     * talking to Backend on AWS), but it <i>must</i> be paired with the <b>Secure</b> flag.</p>
     */
    public void addTokenCookie(ServerWebExchange exchange, String token, Integer maxAgeSeconds) {
        int age = (maxAgeSeconds != null) ? maxAgeSeconds : defaultMaxAgeSeconds;

        ResponseCookie cookie = ResponseCookie
                .from(cookieName, token)
                .httpOnly(true) // Crucial: JS cannot touch this cookie
                .secure(secure) // Only sent over HTTPS
                .path("/")
                .maxAge(Duration.ofSeconds(age))
                // If secure (HTTPS), use 'None' for cross-domain. Otherwise, 'Lax' for local dev.
                .sameSite(secure ? "None" : "Lax")
                .build();

        exchange.getResponse().addCookie(cookie);
    }

    /**
     * Instructs the browser to delete the session cookie immediately.
     */
    public void clearTokenCookie(ServerWebExchange exchange) {
        ResponseCookie cookie = ResponseCookie
                .from(cookieName, "")
                .httpOnly(true)
                .secure(secure)
                .path("/")
                .maxAge(Duration.ZERO) // Expires the cookie instantly
                .sameSite(secure ? "None" : "Lax")
                .build();

        exchange.getResponse().addCookie(cookie);
    }
}