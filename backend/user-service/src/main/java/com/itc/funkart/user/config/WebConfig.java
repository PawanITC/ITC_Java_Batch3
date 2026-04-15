package com.itc.funkart.user.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcRegistrations;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.lang.reflect.Method;

/**
 * Global Web configuration for the User Service.
 * <p>
 * This configuration utilizes {@link WebMvcRegistrations} to globally prepend
 * an API version prefix to all controllers annotated with {@link RestController}.
 * This approach is more resilient than standard path matching as it explicitly
 * overrides the mapping registration process, ensuring consistency across
 * production and test environments.
 * </p>
 *
 * @author Abbas
 * @version 1.2
 */
@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final ApiConfig apiConfig;

    /**
     * Customizes the Request Mapping process to inject global versioning.
     * * @return A custom {@link WebMvcRegistrations} bean that applies the
     * API version prefix from {@link ApiConfig}.
     */
    @Bean
    public WebMvcRegistrations webMvcRegistrations() {
        return new WebMvcRegistrations() {
            @Override
            public RequestMappingHandlerMapping getRequestMappingHandlerMapping() {
                return new RequestMappingHandlerMapping() {
                    @Override
                    protected void registerHandlerMethod(@NonNull Object handler, @NonNull Method method, @NonNull RequestMappingInfo mapping) {
                        Class<?> beanType = method.getDeclaringClass();

                        if (beanType.isAnnotationPresent(RestController.class)) {
                            // Ensure the version string starts with a slash
                            String version = apiConfig.getVersion();
                            String prefix = "/api/" + version;

                            RequestMappingInfo prefixedMapping = RequestMappingInfo
                                    .paths(prefix)
                                    .build()
                                    .combine(mapping);
                            super.registerHandlerMethod(handler, method, prefixedMapping);
                        } else {
                            super.registerHandlerMethod(handler, method, mapping);
                        }
                    }
                };
            }
        };
    }
}