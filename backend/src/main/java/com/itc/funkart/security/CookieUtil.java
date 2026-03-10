package com.itc.funkart.security;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class CookieUtil {

    private final String cookieName;
    private final boolean secure;

    public CookieUtil(@Value("${app.secure-cookie:false}") boolean secure,
                      @Value("${app.cookie-name:token}") String cookieName) {
        this.secure = secure;        // true in prod HTTPS
        this.cookieName = cookieName;
    }

    /** Add or refresh JWT token cookie */
    public void addTokenCookie(HttpServletResponse response, String token, int maxAgeSeconds) {
        Cookie cookie = new Cookie(cookieName, token);
        cookie.setHttpOnly(true);
        cookie.setSecure(secure);
        cookie.setPath("/");
        cookie.setMaxAge(maxAgeSeconds);

        if (secure) {
            cookie.setAttribute("SameSite", "Lax"); // prod: safe default
        } // dev: leave default, avoids issues over HTTP

        response.addCookie(cookie);
    }

    /** Clear JWT token cookie */
    public void clearTokenCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie(cookieName, null);
        cookie.setHttpOnly(true);
        cookie.setSecure(secure);
        cookie.setPath("/");
        cookie.setMaxAge(0);

        if (secure) {
            cookie.setAttribute("SameSite", "Lax");
        }

        response.addCookie(cookie);
    }
}