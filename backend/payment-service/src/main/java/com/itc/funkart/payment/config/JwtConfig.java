package com.itc.funkart.payment.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration Properties for JSON Web Tokens.
 * <p>
 * This class maps properties from application.yaml into a type-safe Java object.
 * It provides the raw materials (Secret and TTL) required for token validation.
 * </p>
 */
@Configuration
@Getter
public class JwtConfig {
    /* * DEV NOTE: @Value vs @ConfigurationProperties
     * We use @Value here for simple, individual fields.
     * The 'secret' is used to sign the tokens so they can't be tampered with.
     * The 'expirationMs' determines how long a user stays logged in.
     */
    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration-ms}")
    private Long expirationMs;
}