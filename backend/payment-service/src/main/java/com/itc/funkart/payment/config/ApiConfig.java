package com.itc.funkart.payment.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Global API Strategy Configuration.
 * <p>
 * Centralizes the management of API versioning and prefixes.
 * This class allows us to change the entire app from /v1 to /v2
 * simply by editing a single line in our application.yaml.
 * </p>
 */
@Setter
@Getter
@Configuration
@ConfigurationProperties(prefix = "api")
public class ApiConfig {

    /* * JUNIOR DEV NOTE: Type-Safe Properties
     * Instead of using @Value in 10 different files, we bind the 'api'
     * prefix from YAML directly to this object. This makes the code
     * much more maintainable and less prone to typos.
     */
    private String version;

}