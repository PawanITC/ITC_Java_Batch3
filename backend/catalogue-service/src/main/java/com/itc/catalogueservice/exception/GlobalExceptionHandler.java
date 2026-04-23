package com.itc.catalogueservice.exception;

import com.itc.catalogueservice.exception.catalogue.NoProductsException;
import com.itc.catalogueservice.response.ApiResponse;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.concurrent.CompletionException;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeoutException;


@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    //Handles no products
    @ExceptionHandler(NoProductsException.class)
    public ResponseEntity<ApiResponse<?>> handleNoProductsException(NoProductsException ex) {

        log.error("Products not available",ex);

        ApiResponse<?> response =
                new ApiResponse<>(HttpStatus.NOT_FOUND.value(), ex.getMessage(), null);

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }


    //Handles constraints
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<?>> handleConstraintViolation(ConstraintViolationException ex) {

        log.error("Validation failed",ex);

        ApiResponse<?> response =
                new ApiResponse<>(HttpStatus.BAD_REQUEST.value(),
                        ex.getConstraintViolations().iterator().next().getMessage(),
                        null);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    //Rate limiter
    @ExceptionHandler(RequestNotPermitted.class)
    public ResponseEntity<ApiResponse<?>> handleRateLimit(RequestNotPermitted ex) {

        log.error("Rate limit exceeded", ex);

        ApiResponse<?> response =
                new ApiResponse<>(HttpStatus.TOO_MANY_REQUESTS.value(),
                        "Rate limit exceeded. Please try again later",
                        null);

        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(response);
    }



    // Timeout → external call too slow (TimeLimiter)
    @ExceptionHandler(TimeoutException.class)
    public ResponseEntity<ApiResponse<?>> handleTimeout(TimeoutException ex) {

        log.error("External service timeout", ex);

        ApiResponse<?> response =
                new ApiResponse<>(HttpStatus.GATEWAY_TIMEOUT.value(),
                        "External service timed out",
                        null);

        return ResponseEntity.status(HttpStatus.GATEWAY_TIMEOUT).body(response);
    }


    // Executor overload → too many concurrent requests
    @ExceptionHandler(RejectedExecutionException.class)
    public ResponseEntity<ApiResponse<?>> handleBulkhead(RejectedExecutionException ex) {

        log.error("Too many concurrent requests", ex);

        ApiResponse<?> response =
                new ApiResponse<>(HttpStatus.TOO_MANY_REQUESTS.value(),
                        "Too many concurrent requests",
                        null);

        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(response);
    }


    // Circuit breaker → service failing repeatedly
    @ExceptionHandler(CallNotPermittedException.class)
    public ResponseEntity<ApiResponse<?>> handleCircuit(CallNotPermittedException ex) {

        log.error("Circuit breaker open", ex);

        ApiResponse<?> response =
                new ApiResponse<>(HttpStatus.SERVICE_UNAVAILABLE.value(),
                        "Service temporarily unavailable",
                        null);

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
    }

    // Handles async exceptions from TimeLimiter (CompletableFuture wraps exceptions)
    @ExceptionHandler(java.util.concurrent.CompletionException.class)
    public ResponseEntity<ApiResponse<?>> handleCompletionException(CompletionException ex) {

        // Extract actual root cause (e.g. TimeoutException)
        Throwable cause = ex.getCause();

        // If caused by timeout → return 504 Gateway Timeout
        if (cause instanceof TimeoutException) {
            ApiResponse<?> response =
                    new ApiResponse<>(HttpStatus.GATEWAY_TIMEOUT.value(),
                            "External service timed out",
                            null);

            return ResponseEntity.status(HttpStatus.GATEWAY_TIMEOUT).body(response);
        }

        // If not timeout → rethrow and let other handlers deal with it
        throw ex;
    }

}
