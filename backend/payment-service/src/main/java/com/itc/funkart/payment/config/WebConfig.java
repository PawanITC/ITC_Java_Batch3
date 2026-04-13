package com.itc.funkart.payment.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.method.HandlerTypePredicate;
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
        // Fallback to "api/v1" if the mock/config returns null
        String version = (apiConfig != null && apiConfig.getVersion() != null)
                ? apiConfig.getVersion()
                : "api/v1";

        configurer.addPathPrefix("/" + version,
                HandlerTypePredicate.forAnnotation(RestController.class));
    }
}