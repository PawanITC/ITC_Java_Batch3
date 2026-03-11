package com.itc.funkart.security;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class CookieUtil {

    private final String cookieName;
    private final boolean secure;
    @Value("${jwt.cookie-max-age-seconds}")
    private int defaultMaxAgeSeconds;

    public CookieUtil(@Value("${app.secure-cookie:false}") boolean secure,
                      @Value("${app.cookie-name:token}") String cookieName) {
        this.secure = secure;        // true in prod HTTPS
        this.cookieName = cookieName;
    }

    public String getCookieName() {
        return cookieName;
    }

    /**
     * Add or refresh JWT token cookie
     */
    public void addTokenCookie(HttpServletResponse response,
                               String token, Integer maxAgeSeconds) {
        int age = (maxAgeSeconds != null) ? maxAgeSeconds : defaultMaxAgeSeconds;

        Cookie cookie = new Cookie(cookieName, token);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(age);

        if (secure) {
            cookie.setSecure(true);
            cookie.setAttribute("SameSite", "None");
        } else {
            cookie.setSecure(false);
            cookie.setAttribute("SameSite", "Lax");
        }

        response.addCookie(cookie);
    }

    /**
     * Clear JWT token cookie
     */
    public void clearTokenCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie(cookieName, null);
        cookie.setHttpOnly(true);
        cookie.setSecure(secure);
        cookie.setPath("/");
        cookie.setMaxAge(0);

        if (secure) {
            cookie.setAttribute("SameSite", "None");
        } else {
            cookie.setAttribute("SameSite", "Lax");
        }

        response.addCookie(cookie);
    }
}