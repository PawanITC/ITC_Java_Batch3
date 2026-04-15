package com.itc.funkart.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * <h2>WebClient Configuration</h2>
 * <p>
 * Central factory for WebClient instances used to communicate with downstream services.
 * Resolves service base URLs via {@link ServiceRegistry}, ensuring a single source of truth.
 * </p>
 */
@Configuration
public class WebClientConfig {

    private final ServiceRegistry serviceRegistry;

    public WebClientConfig(ServiceRegistry serviceRegistry) {
        this.serviceRegistry = serviceRegistry;
    }

    /**
     * Creates a WebClient bound to a specific service base URL.
     *
     * @param baseUrl resolved service base URL from ServiceRegistry
     * @return configured WebClient instance
     */
    public WebClient createClient(String baseUrl) {
        return WebClient.builder()
                .baseUrl(baseUrl)
                .build();
    }

    /**
     * Primary WebClient for the User Service.
     */
    @Bean
    @Primary
    public WebClient userWebClient() {
        return createClient(serviceRegistry.userService());
    }

    /**
     * WebClient for the Payment Service.
     */
    @Bean
    public WebClient paymentWebClient() {
        return createClient(serviceRegistry.paymentService());
    }

    /**
     * WebClient for the Order Service.
     */
    @Bean
    public WebClient orderWebClient() {
        return createClient(serviceRegistry.orderService());
    }
}