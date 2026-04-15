package com.itc.funkart.gateway.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.method.HandlerTypePredicate;
import org.springframework.web.reactive.config.PathMatchConfigurer;
import org.springframework.web.reactive.config.WebFluxConfigurer;

/**
 * <h2>Global Routing & Prefix Configuration</h2>
 * <p>Automatically prepends the API version (e.g., /api/v1) to all controllers.
 * This avoids hardcoding versions in every Controller class.</p>
 */
@Configuration
public class WebConfig implements WebFluxConfigurer {

    private final AppConfig appConfig;

    public WebConfig(AppConfig appConfig) {
        this.appConfig = appConfig;
    }

    @Override
    public void configurePathMatching(PathMatchConfigurer configurer) {
        String prefix = appConfig.api().version();

        configurer.addPathPrefix(prefix,
                HandlerTypePredicate.forAnnotation(RestController.class)
                        .and(handlerType -> {
                            // 1. Must have @RequestMapping
                            RequestMapping mapping = handlerType.getAnnotation(RequestMapping.class);
                            boolean hasMapping = (mapping != null && mapping.value().length > 0);

                            // 2. Must NOT have @NoApiPrefix
                            boolean isNotExcluded = !handlerType.isAnnotationPresent(NoApiPrefix.class);

                            // 3. (Optional) Manual exclusion for OAuth if not using the annotation
                            boolean isNotOAuth = hasMapping && !mapping.value()[0].startsWith("/oauth");

                            return hasMapping && isNotExcluded && isNotOAuth;
                        })
        );
    }
}