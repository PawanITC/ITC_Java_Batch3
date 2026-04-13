package com.itc.funkart.user.config;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for versioning the REST API.
 * Mapped from the {@code api} prefix in the application properties.
 * * @param version The current API version (e.g., {@code v1}).
 */
@Getter
@RequiredArgsConstructor
@ConfigurationProperties(prefix = "api")
public class ApiConfig {
    // Final field ensures immutability
    private final String version;

    // Default value if the property is missing in yml
    public ApiConfig() {
        this.version = "v1";
    }
}