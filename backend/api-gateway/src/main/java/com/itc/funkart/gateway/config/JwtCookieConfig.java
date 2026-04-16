package com.itc.funkart.gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.jwt")
public record JwtCookieConfig(
        String cookieName,
        boolean secureCookie,
        int cookieMaxAgeSeconds
) {}
