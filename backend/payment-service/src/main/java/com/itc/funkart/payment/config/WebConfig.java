package com.itc.funkart.payment.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Global Web Configuration for the Payment Microservice.
 * <p>
 * This class implements {@link WebMvcConfigurer} to customize how Spring MVC
 * maps incoming HTTP requests to our Controllers.
 * </p>
 * <p><b>Feature: Global API Versioning</b></p>
 * Instead of hardcoding "/api/v1" in every @RequestMapping, this configuration
 * automatically prepends the version prefix defined in {@code application.yaml}
 * to all classes annotated with {@code @RestController}.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final ApiConfig apiConfig;

    public WebConfig(ApiConfig apiConfig) {
        this.apiConfig = apiConfig;
    }

    /**
     * Configures path matching options.
     * * @param configurer The PathMatchConfigurer to customize.
     */
    @Override
    public void configurePathMatch(PathMatchConfigurer configurer) {
        // 1. Retrieve the prefix from YAML (e.g., "api/v1")
        // We add a leading slash to ensure valid URL construction: "/api/v1"
        String prefix = "/" + apiConfig.getVersion();

        // 2. Apply the prefix dynamically
        // The lambda expression 'c -> c.isAnnotationPresent(RestController.class)'
        // ensures this prefix ONLY applies to API controllers.
        // This keeps system endpoints like '/actuator/health' or '/swagger-ui'
        // at the root, which is standard for monitoring tools.
        configurer.addPathPrefix(prefix, beanType -> beanType.isAnnotationPresent(RestController.class));
    }
}