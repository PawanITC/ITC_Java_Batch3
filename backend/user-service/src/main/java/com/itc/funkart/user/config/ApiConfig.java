package com.itc.funkart.user.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for versioning the REST API.
 * Mapped from the {@code api} prefix in the application properties.
 * * @param version The current API version (e.g., {@code v1}).
 */
@Setter
@Getter
@Configuration
@ConfigurationProperties(prefix = "api")
public class ApiConfig {
    private String version;
}