package com.itc.funkart.user.config;

import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for versioning the REST API.
 * Mapped from the {@code api} prefix in the application properties.
 * * @param version The current API version (e.g., {@code v1}).
 */
@Getter
@ConfigurationProperties(prefix = "api")
public class ApiConfig {
    private final String version;

    public ApiConfig(String version) {
        this.version = version;
    }
}