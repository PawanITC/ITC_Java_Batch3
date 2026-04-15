package com.itc.funkart.gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.springframework.validation.annotation.Validated;
import java.util.Map;

/**
 * <h2>Root configuration record for the Funkart API Gateway.</h2>
 * <p>
 * This record acts as the centralized <b>Source of Truth</b> for the entire Funkart ecosystem.
 * It uses <b>Constructor Binding</b> to map properties from {@code application.yaml} or
 * environment variables into a type-safe, immutable object tree.
 * </p>
 * * @param frontendUrl The URL of the client app (React/Vite) used for CORS policy.
 * @param api         Metadata for path versioning (e.g., /api/v1).
 * @param jwt         Security settings for token signing and cookie management.
 * @param github      OAuth credentials for GitHub social login integration.
 * @param services    A dynamic registry of internal microservice URLs.
 * Key: Service Name (e.g., "order-service"), Value: Target URL.
 */
@Validated
@ConfigurationProperties(prefix = "app")
public record AppConfig(
        @NotBlank String frontendUrl,
        @NotNull Api api,
        @NotNull Jwt jwt,
        @NotNull Github github,
        @NotEmpty Map<String, String> services
) {

    /**
     * API metadata and path versioning.
     * @param version The version string, usually {@code /api/v1}.
     */
    public record Api(@NotBlank String version) {}

    /**
     * Security configuration for JWT processing.
     * @param secret              Base64 encoded key for HS256 integrity checks.
     * @param expirationMs        Validity duration (ms).
     * @param cookieMaxAgeSeconds Persistence duration in the browser.
     * @param cookieName          The key name (e.g., "token").
     * @param secureCookie        Set to TRUE for HTTPS/Production environments.
     */
    public record Jwt(
            @NotBlank String secret,
            @NotNull Long expirationMs,
            @NotNull Integer cookieMaxAgeSeconds,
            @NotBlank String cookieName,
            boolean secureCookie
    ) {}

    /**
     * GitHub OAuth credentials.
     * @param clientId     Generated via GitHub Developer Settings.
     * @param clientSecret exchange authorization codes for tokens.
     * @param redirectUri  Authorized callback URL.
     */
    public record Github(
            @NotBlank String clientId,
            @NotBlank String clientSecret,
            @NotBlank String redirectUri
    ) {}
}