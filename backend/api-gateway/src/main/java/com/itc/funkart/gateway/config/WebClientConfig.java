package com.itc.funkart.gateway.config;

import io.netty.channel.ChannelOption;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

import java.time.Duration;

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
     * Creates a highly-optimized, non-blocking WebClient bound to a specific service base URL.
     * * <p>Performance & Reliability Features:</p>
     * <ul>
     * <li><b>Connection Pooling:</b> Uses a dedicated "gateway-pool" with a 500-connection limit
     * to prevent socket exhaustion during high-load tests.</li>
     * <li><b>Resilience Timeouts:</b> Implements a 5s connect timeout and a 10s response timeout
     * to prevent downstream service latency from cascading through the gateway.</li>
     * <li><b>Memory Safety:</b> Increases the default codec buffer to 16MB to handle large
     * JSON payloads without {@code DataBufferLimitException}.</li>
     * </ul>
     *
     * @param baseUrl The resolved base URL of the downstream microservice (e.g. http://user-service:8080).
     * @return A configured {@link WebClient} instance with Netty-level optimizations.
     * @see ConnectionProvider
     */
    public WebClient createClient(String baseUrl) {
        // 1. Configure a dedicated connection pool for high-load
        ConnectionProvider provider = ConnectionProvider.builder("gateway-pool")
                .maxConnections(500)          // Up from the default 2k (per-route) to manage overall limit
                .pendingAcquireTimeout(Duration.ofSeconds(60)) // How long to wait for a connection from the pool
                .maxIdleTime(Duration.ofSeconds(20))           // Cleanup old connections
                .build();

        // 2. Configure the underlying Netty HttpClient
        HttpClient httpClient = HttpClient.create(provider) // Pass the provider here
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
                .responseTimeout(Duration.ofSeconds(10))
                .doOnConnected(conn ->
                        conn.addHandlerLast(new io.netty.handler.timeout.ReadTimeoutHandler(10))
                                .addHandlerLast(new io.netty.handler.timeout.WriteTimeoutHandler(10)));

        // 3. Build the WebClient
        return WebClient.builder()
                .baseUrl(baseUrl)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(16 * 1024 * 1024))
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