package com.itc.funkart.exception;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleRateLimit_shouldReturn429() {
        ResponseEntity<String> response = handler.handleRateLimit(mock(RequestNotPermitted.class));
        assertEquals(429, response.getStatusCode().value());
        assertEquals("Too Many Requests - limit exceeded", response.getBody());
    }

    @Test
    void handleOrderFound_shouldReturn404() {
        ResponseEntity<String> response = handler.handleOrderFound(new OrderNotFound("missing"));
        assertEquals(404, response.getStatusCode().value());
        assertEquals("missing", response.getBody());
    }

    @Test
    void handleCircuitBreakerOpen_shouldReturn503() {
        ResponseEntity<String> response = handler.handleCircuitBreakerOpen(
                mock(CallNotPermittedException.class)
        );
        assertEquals(503, response.getStatusCode().value());
        assertEquals("Service unavailable - Circuit breaker is OPEN", response.getBody());
    }
}
