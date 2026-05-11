package com.itc.funkart.aggregator.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Secure configuration for JWT processing within the Product Service.
 * <p>
 * This class maps externalized configuration properties (from application.yml or ENV)
 * into usable beans for the cryptographic engine.
 * </p>
 * <p>
 * <b>Security Note:</b> The 'secret' must be a Base64 encoded string of at least 256 bits
 * to support the HS256 algorithm.
 * </p>
 */
@Configuration
@Getter
public class JwtConfig {

    /**
     * The shared Base64 encoded secret key used for signature verification.
     */
    @Value("${jwt.secret}")
    private String secret;

    /**
     * The duration (in milliseconds) for which a token is considered valid.
     */
    @Value("${jwt.expiration-ms}")
    private Duration expirationMs;
}