package com.itc.funkart.gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.validation.annotation.Validated;

/**
 * Root configuration record for the Funkart API Gateway.
 * <p>
 * This class acts as the centralized "Source of Truth" for all environment-specific
 * settings. It maps properties from {@code application.yaml} or system environment
 * variables (e.g., {@code APP_FRONTENDURL}) into a type-safe hierarchy.
 * </p>
 * <p>
 * This record uses <b>Constructor Binding</b>, meaning the values are injected
 * during the instantiation of the bean at startup.
 * </p>
 *
 * @param frontendUrl The base URL where the Vite/React frontend is hosted (used for CORS and redirects).
 * @param api         Metadata regarding API versioning and prefixes.
 * @param jwt         Security settings for JSON Web Token signature verification and cookie policy.
 * @param github      Credentials and URIs required for the GitHub OAuth2 handshake.
 */
@Validated // Ensures @NotBlank is checked on startup
@ConfigurationProperties(prefix = "app")
public record AppConfig(
        @NotBlank String frontendUrl,
        @NotNull Api api,
        @NotNull Jwt jwt,
        @NotNull Github github,
        @NotNull Services services
) {

    /**
     * API metadata and path versioning.
     * * @param version The API prefix used for routing and security matching (e.g., "/api/v1").
     */
    public record Api(@NotBlank String version) {}

    /**
     * Security configuration for JWT processing.
     * * @param secret              The Base64 encoded secret key used by JJWT to verify token integrity.
     * @param expirationMs        Token validity duration in milliseconds.
     * @param cookieMaxAgeSeconds The duration the browser should persist the JWT cookie.
     * @param cookieName          The key name for the JWT cookie (e.g., "token").
     * @param secureCookie        Flag to enable 'Secure' and 'SameSite=None' for HTTPS environments.
     */
    public record Jwt(
            @NotBlank String secret,
            @NotNull Long expirationMs,
            @NotNull Integer cookieMaxAgeSeconds,
            @NotBlank String cookieName,
            boolean secureCookie
    ) {}

    /**
     * Configuration for GitHub OAuth integration.
     * * @param clientId     The Client ID generated in the GitHub Developer Settings.
     * @param clientSecret The sensitive Client Secret used to exchange authorization codes for tokens.
     * @param redirectUri  The authorized callback URL registered with GitHub.
     */
    public record Github(
            @NotBlank String clientId,
            @NotBlank String clientSecret,
            @NotBlank String redirectUri
    ) {}

    /**
     * Internal microservice location settings.
     * @param userServiceUrl The internal URI for the user management service.
     */
    public record Services(@NotBlank String userServiceUrl) {}
}