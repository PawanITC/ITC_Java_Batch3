package com.itc.funkart.payment.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

/**
 * <h2>RestClientConfig</h2>
 * <p>
 * Configures the primary engine for synchronous "Service-to-Service" (East-West) communication.
 * </p>
 * <p>
 * This configuration produces a thread-safe {@link RestClient} bean, which is the
 * standardized HTTP client for Spring 6.1+. It replaces the deprecated RestTemplate
 * with a more functional, fluent API.
 * </p>
 *
 * <b>Resilience Strategy:</b>
 * <p>
 * To prevent <i>Cascading Failures</i>, this client is configured with strict
 * connection and read timeouts. This ensures that if a downstream service (like Order-Service)
 * becomes unresponsive, the Payment-Service will fail fast rather than exhausting its thread pool.
 * </p>
 */
@Configuration
public class RestClientConfig {

    /**
     * Provides a pre-configured {@link RestClient} with sensible default timeouts.
     *
     * @return A RestClient capable of making resilient outbound HTTP calls.
     */
    @Bean
    public RestClient restClient() {

        // Use the modern JDK HttpClient with an explicit connection timeout.
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder()
                        .connectTimeout(Duration.ofSeconds(5)) // Establishment window
                        .build()
        );

        // Set the maximum time to wait for data packets once the connection is open.
        factory.setReadTimeout(Duration.ofSeconds(10));

        return RestClient.builder()
                .requestFactory(factory)
                .build();
    }
}