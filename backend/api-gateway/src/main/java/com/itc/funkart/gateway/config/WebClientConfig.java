package com.itc.funkart.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Bean
    public WebClient webClient(AppConfig appConfig) {
        // Check if services or the URL is missing to provide a clearer error or default
        String baseUrl = (appConfig.services() != null)
                ? appConfig.services().userServiceUrl()
                : "http://localhost:8081"; // Fallback for local dev/testing

        return WebClient.builder()
                .baseUrl(baseUrl)
                .build();
    }
}