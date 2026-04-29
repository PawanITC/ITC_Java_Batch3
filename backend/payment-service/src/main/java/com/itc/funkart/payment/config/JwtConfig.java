package com.itc.funkart.payment.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * <h2>JwtConfig</h2>
 * <p>
 * Secure configuration for JWT processing.
 * </p>
 * <p>
 * <b>Security Note:</b> The 'secret' should ideally be stored in a
 * Secure Vault or Environment Variable, never hardcoded in YAML.
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