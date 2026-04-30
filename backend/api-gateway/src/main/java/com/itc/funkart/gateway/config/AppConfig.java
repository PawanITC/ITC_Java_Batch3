package com.itc.funkart.gateway.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.Map;

/**
 * Root configuration for the API Gateway.
 *
 * <p>
 * This class is strictly for environment configuration and secrets.
 * It MUST NOT contain routing/versioning logic (handled by Gateway).
 * </p>
 */
@Validated
@ConfigurationProperties(prefix = "app")
public record AppConfig(

        @NotBlank(message = "Frontend URL must be configured (APP_FRONTEND_URL)")
        String frontendUrl,

        @NotBlank(message = "OAuth success path must be configured")
        String oauthSuccessPath,

        @NotNull
        Jwt jwt,

        @NotNull
        Github github,

        @NotEmpty
        Map<String, String> services
) {

    /**
     * Helper to resolve service URLs from the internal map.
     * This is the bridge used by AppServiceRegistry.
     */
    public String getServiceUrl(String serviceName) {
        String url = services.get(serviceName);
        // Supports relaxed binding (user-service vs userservice)
        if (url == null || url.isBlank()) {
            url = services.get(serviceName.replace("-", ""));
        }
        if (url == null || url.isBlank()) {
            throw new IllegalStateException("Service URL missing for: " + serviceName);
        }
        return url;
    }

    /**
     * JWT security configuration.
     */
    public record Jwt(
            @NotBlank String secret,
            @NotNull Long expirationMs,
            @NotNull Integer cookieMaxAgeSeconds,
            @NotBlank String cookieName,
            boolean secureCookie
    ) {
    }

    /**
     * GitHub OAuth configuration.
     */
    public record Github(
            @NotBlank String clientId,
            @NotBlank String clientSecret,
            @NotBlank String redirectUri
    ) {
    }
}