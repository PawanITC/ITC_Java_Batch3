package com.itc.funkart.payment.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

/**
 * Shared Infrastructure for Synchronous HTTP Communication.
 * <p>
 * This configuration provides a centralized {@link RestClient} bean, which is the
 * modern Spring 6.1+ replacement for the legacy RestTemplate. It is used for
 * "Service-to-Service" calls (e.g., asking the Order-Service for a total).
 * </p>
 * * @author Abbas (Funkart Team)
 */
@Configuration
public class RestClientConfig {

    /**
     * Creates a managed RestClient bean with built-in resilience.
     * * @return A thread-safe, pre-configured RestClient.
     */
    @Bean
    public RestClient restClient() {

        /* * DEV NOTE:
         * We use JdkClientHttpRequestFactory here to set explicit "Timeouts."
         * By default, many HTTP clients will wait forever for a response.
         * If the 'Order Service' is lagging, we don't want our 'Payment Service'
         * threads to hang indefinitely, which would eventually crash our app.
         */
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder()
                        .connectTimeout(Duration.ofSeconds(5)) // Time to establish the connection
                        .build()
        );

        /* * We set a Read Timeout of 10 seconds.
         * If the downstream service doesn't send data back within this window,
         * we throw an exception rather than holding up resources.
         */
        factory.setReadTimeout(Duration.ofSeconds(10));

        /*
         * RestClient.builder() allows us to add global features like
         * default headers (e.g., Content-Type: application/json) or
         * ErrorHandlers in the future without changing individual service calls.
         */
        return RestClient.builder()
                .requestFactory(factory)
                .build();
    }
}