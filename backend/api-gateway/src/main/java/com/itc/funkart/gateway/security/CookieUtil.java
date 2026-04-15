package com.itc.funkart.gateway.security;

import com.itc.funkart.gateway.config.AppConfig;
import lombok.Getter;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import java.time.Duration;

/**
 * Utility component for managing JWT-related cookies in a reactive environment.
 * <p>
 * This class abstracts the complexities of Cookie construction, ensuring consistent
 * application of security flags like {@code HttpOnly}, {@code Secure}, and {@code SameSite}.
 * It relies on {@link AppConfig} for centralized environment-specific settings.
 * </p>
 */
@Component
public class CookieUtil {

    private static final String SAME_SITE_LAX = "Lax";
    private static final String SAME_SITE_NONE = "None";

    @Getter
    private final String cookieName;
    private final boolean secure;
    private final int defaultMaxAgeSeconds;

    /**
     * Constructs the utility using the centralized application configuration.
     * * @param appConfig The source of truth for security and cookie settings.
     */
    public CookieUtil(AppConfig appConfig) {
        this.cookieName = appConfig.jwt().cookieName();
        this.secure = appConfig.jwt().secureCookie();
        this.defaultMaxAgeSeconds = appConfig.jwt().cookieMaxAgeSeconds();
    }

    /**
     * Adds a new JWT token cookie to the response or refreshes an existing one.
     * <p>
     * If the environment is marked as secure (HTTPS), it applies {@code SameSite=None}
     * to allow cross-site cookie usage, which is common in decoupled Gateway-Frontend architectures.
     * </p>
     *
     * @param exchange      The current server exchange.
     * @param token         The JWT string to be stored.
     * @param maxAgeSeconds Optional override for cookie expiration; defaults to config value if null.
     */
    public void addTokenCookie(ServerWebExchange exchange, String token, Integer maxAgeSeconds) {
        int age = (maxAgeSeconds != null) ? maxAgeSeconds : defaultMaxAgeSeconds;

        ResponseCookie cookie = ResponseCookie
                .from(cookieName, token)
                .httpOnly(true)
                .path("/")
                .maxAge(Duration.ofSeconds(age))
                .sameSite(secure ? SAME_SITE_NONE : SAME_SITE_LAX)
                .secure(secure)
                .build();

        exchange.getResponse().addCookie(cookie);
    }

    /**
     * Effectively logs out the user by adding a zero-lifetime cookie to the response.
     * This instructs the browser to immediately delete the stored JWT.
     *
     * @param exchange The current server exchange.
     */
    public void clearTokenCookie(ServerWebExchange exchange) {
        ResponseCookie cookie = ResponseCookie
                .from(cookieName, "")
                .httpOnly(true)
                .path("/")
                .maxAge(Duration.ZERO)
                .sameSite(secure ? SAME_SITE_NONE : SAME_SITE_LAX)
                .secure(secure)
                .build();

        exchange.getResponse().addCookie(cookie);
    }
}