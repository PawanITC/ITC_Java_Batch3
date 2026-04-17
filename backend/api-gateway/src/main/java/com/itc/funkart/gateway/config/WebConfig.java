package com.itc.funkart.gateway.config;

import com.itc.funkart.gateway.config.props.ApiProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.RestController;
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

    private final ApiProperties apiProperties;

    public WebConfig(ApiProperties apiProperties) {
        this.apiProperties = apiProperties;
    }

    @Override
    public void configurePathMatching(@NonNull PathMatchConfigurer configurer) {
        String prefix = apiProperties.version();

        // Defensive check: Ensure prefix isn't null/empty and starts with /
        if (prefix != null && !prefix.isBlank()) {
            final String sanitizedPrefix = prefix.startsWith("/") ? prefix : "/" + prefix;

            configurer.addPathPrefix(sanitizedPrefix,
                    HandlerTypePredicate.forAnnotation(RestController.class)
                            .and(handlerType ->
                                    handlerType.getAnnotation(NoApiPrefix.class) == null
                            )
            );
        }
    }
}