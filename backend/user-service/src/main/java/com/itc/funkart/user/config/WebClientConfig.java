package com.itc.funkart.user.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Configuration for the non-blocking {@link WebClient}.
 * Primarily used by {@code GithubOAuthService} for external API communication.
 */
@Configuration
public class WebClientConfig {

    /**
     * Provides a standard {@link WebClient.Builder} instance.
     * * @return A shared, thread-safe {@code WebClient} for the application context.
     */
    @Bean
    public WebClient webClient() {
        return WebClient.builder().build();
    }
}