package com.itc.funkart.gateway.config.props;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "jwt")
public record JwtProperties(
        String secret,
        Long expirationMs,
        Integer cookieMaxAgeSeconds,
        String cookieName,
        boolean secureCookie
) {}