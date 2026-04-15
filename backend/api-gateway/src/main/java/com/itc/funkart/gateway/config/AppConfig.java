package com.itc.funkart.gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.validation.annotation.Validated;

/**
 * <h2>Root configuration record for the Funkart API Gateway.</h2>
 * <h4>The Funkart Gateway "Source of Truth"</h5>
 *
 * </p>
 * <p>
 * This record uses <b>Constructor Binding</b>, meaning the values are injected
 * during the instantiation of the bean at startup.
 * </p>
 * This class acts as the centralized "Source of Truth" for all environment-specific
 * settings. It maps properties from {@code application.yaml} or system environment
 * variables (e.g. {@code APP_FRONTENDURL}) into a type-safe hierarchy.
 *
 * <p>
 *
 * <p>
 * <p>In plain English, this record maps your {@code application.yml} into a type-safe object tree.</p>
 * Using Records ensures that once the Gateway starts, its configuration cannot be
 * modified (Immutability).</p>
 * @param frontendUrl The URL of the client app (React/Vite). Used to solve CORS issues.
 * @param api         Path metadata (e.g., /api/v1).
 * @param jwt         Security keys and cookie settings.
 * @param github      OAuth credentials for social login.
 * @param services    Internal URLs for our microservices (e.g., user-service).
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
     * @param version The version string, usually {@code v1} or {@code /api/v1}.
     * */
    public record Api(@NotBlank String version) {}

    /**
     * Security configuration for JWT processing.
     * @param secret              The Base64 encoded secret key used by JJWT to verify token integrity. The signing key. Must be at least 256-bit for HS256.
     * @param expirationMs        Token validity duration in milliseconds.
     * @param cookieMaxAgeSeconds The duration the browser should persist the JWT cookie.
     * @param cookieName          The key name for the JWT cookie (e.g., "token").
     * @param secureCookie        Flag to enable 'Secure' and 'SameSite=None' for HTTPS environments. secureCookie Should be TRUE in production (HTTPS) and FALSE in local dev (HTTP).
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
     * @param userServiceUrl The internal DNS name or IP of the User microservice.
     */
    public record Services(@NotBlank String userServiceUrl) {}
}