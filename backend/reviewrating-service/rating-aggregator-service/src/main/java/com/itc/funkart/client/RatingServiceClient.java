package com.itc.funkart.client;

import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.concurrent.CompletableFuture;

@Service
public class RatingServiceClient {

    private final WebClient client;

    public RatingServiceClient(WebClient.Builder builder) {
        this.client = builder.baseUrl("http://review-rating-service:9097").build();
    }

    @CircuitBreaker(name = "ratingServiceCb", fallbackMethod = "fallback")
    @Retry(name = "ratingServiceRetry")
    @TimeLimiter(name = "ratingServiceTl")
    @Bulkhead(name = "ratingServiceBh", type = Bulkhead.Type.THREADPOOL)
    public CompletableFuture<RatingResponse> getRating(String productId) {
        return client.get()
                .uri("/api/v1/ratings/{id}", productId)
                .retrieve()
                .bodyToMono(RatingResponse.class)
                .toFuture();
    }

    private CompletableFuture<RatingResponse> fallback(String productId, Throwable ex) {
        return CompletableFuture.completedFuture(
                new RatingResponse(productId, 0.0, 0L, true)
        );
    }
}