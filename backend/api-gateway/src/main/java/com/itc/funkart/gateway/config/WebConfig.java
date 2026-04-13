package com.itc.funkart.gateway.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.method.HandlerTypePredicate;
import org.springframework.web.reactive.config.PathMatchConfigurer;
import org.springframework.web.reactive.config.WebFluxConfigurer;

@Configuration
public class WebConfig implements WebFluxConfigurer {

    private final AppConfig appConfig;

    public WebConfig(AppConfig appConfig) {
        this.appConfig = appConfig;
    }

    @Override
    public void configurePathMatching(PathMatchConfigurer configurer) {
        String prefix = appConfig.api().version(); // Expects "/api/v1"

        configurer.addPathPrefix(prefix,
                HandlerTypePredicate.forAnnotation(RestController.class)
                        // This is the key: Exclude anything that maps to /oauth
                        .and(predicate -> !predicate.getAnnotation(org.springframework.web.bind.annotation.RequestMapping.class)
                                .value()[0].startsWith("/oauth"))
        );
    }
}