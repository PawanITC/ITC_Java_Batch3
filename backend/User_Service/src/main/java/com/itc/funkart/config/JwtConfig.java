package com.itc.funkart.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JwtConfig {

    private final String secret;
    private final Long expirationMs;

    public JwtConfig(@Value("${jwt.secret}") String secret,
                     @Value("${jwt.expiration-ms}") Long expirationMs) {
        this.secret = secret;
        this.expirationMs = expirationMs;
    }

    public String getSecret() {
        return secret;
    }

    public Long getExpirationMs() {
        return expirationMs;
    }
}
