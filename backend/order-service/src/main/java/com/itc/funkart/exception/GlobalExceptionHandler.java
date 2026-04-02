package com.itc.funkart.exception;

//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.ExceptionHandler;

//public class GlobalExceptionHandler {
//    @ExceptionHandler(RuntimeException.class)
//    public ResponseEntity<String> handleRuntimeException(RuntimeException ex){
//        return ResponseEntity.badRequest().body(ex.getMessage());
//    }


import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;

import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RequestNotPermitted.class)
    public ResponseEntity<String> handleRateLimit(RequestNotPermitted ex) {
        return ResponseEntity
                .status(429)
                .body("Too Many Requests - limit exceeded");
    }
    @ExceptionHandler(OrderNotFound.class)
    public ResponseEntity<String> handleOrderFound(OrderNotFound ex) {
        return ResponseEntity
                .status(404)
                .body(ex.getMessage());
    }

    @ExceptionHandler(CallNotPermittedException.class)
    public ResponseEntity<String> handleCircuitBreakerOpen(CallNotPermittedException ex) {
        return ResponseEntity
                .status(503) // or 429 depending on your design
                .body("Service unavailable - Circuit breaker is OPEN");
    }
}