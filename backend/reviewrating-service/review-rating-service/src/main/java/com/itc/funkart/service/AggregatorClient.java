package com.itc.funkart.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.reactive.function.client.WebClient;

public class AggregatorClient {
    private final WebClient webClient;
    private final String baseUrl;

    public AggregatorClient(WebClient.Builder builder,
                            @Value("${rating-aggregator.base-url}") String baseUrl) {
        this.webClient = builder.build();
        this.baseUrl = baseUrl;
    }

    public Double getAverageRating(String productId) {
        return webClient.get()
                .uri(baseUrl + "/average/" + productId)
                .retrieve()
                .bodyToMono(Double.class)
                .block();
    }

}
