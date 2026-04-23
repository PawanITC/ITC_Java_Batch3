package com.itc.funkart.user.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Security properties for JWT generation and validation.
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "jwt")
public class JwtConfig {
    /**
     * The Base64 encoded signing key.
     */
    private String secret;
    /**
     * Token validity duration in milliseconds.
     */
    private long expirationMs;
}