package com.itc.funkart.gateway.security;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import java.time.Duration;

@Component
public class CookieUtil {

    private static final String SAME_SITE_LAX = "Lax";
    private static final String SAME_SITE_NONE = "None";

    @Getter
    private final String cookieName;
    private final boolean secure;
    private final int defaultMaxAgeSeconds;

    public CookieUtil(@Value("${app.secure-cookie:false}") boolean secure,
                      @Value("${app.cookie-name:token}") String cookieName,
                      @Value("${jwt.cookie-max-age-seconds:3600}") int defaultMaxAgeSeconds) {
        this.secure = secure;        // true in prod HTTPS
        this.cookieName = cookieName;
        this.defaultMaxAgeSeconds = defaultMaxAgeSeconds;
    }

    /**
     * Add or refresh JWT token cookie (reactive)
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
     * Clear JWT token cookie (reactive)
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