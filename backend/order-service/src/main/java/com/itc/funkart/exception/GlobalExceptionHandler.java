package com.itc.funkart.exception;

import com.itc.funkart.common.dto.response.ApiResponse;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Centralized exception handling for the Order Service.
 * Wraps all failures in the standardized ApiResponse<Void> format.
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * Centralized builder to ensure all error responses follow the static factory pattern.
     */
    private ResponseEntity<ApiResponse<Void>> buildErrorResponse(
            HttpStatus status,
            String message,
            String field,
            boolean logStackTrace,
            Exception ex) {

        if (logStackTrace) {
            log.error("Order Service Error [{}]: {}", status.value(), message, ex);
        } else {
            log.warn("Order Service Warning [{}]: {}", status.value(), message);
        }

        // Use the static factory method from your ApiResponse class
        return ResponseEntity.status(status)
                .body(ApiResponse.error(status.name(), message, field));
    }

    // --- Resilience4j Handlers ---

    @ExceptionHandler(RequestNotPermitted.class)
    public ResponseEntity<ApiResponse<Void>> handleRateLimit(RequestNotPermitted ex) {
        return buildErrorResponse(HttpStatus.TOO_MANY_REQUESTS, "Order rate limit exceeded", null, false, ex);
    }

    @ExceptionHandler(CallNotPermittedException.class)
    public ResponseEntity<ApiResponse<Void>> handleCircuitBreaker(CallNotPermittedException ex) {
        return buildErrorResponse(HttpStatus.SERVICE_UNAVAILABLE, "Circuit Breaker open - temporary downtime", null, true, ex);
    }

    // --- Business Logic Handlers ---

    @ExceptionHandler(OrderNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFound(OrderNotFoundException ex) {
        return buildErrorResponse(HttpStatus.NOT_FOUND, ex.getMessage(), "orderId", false, ex);
    }

    @ExceptionHandler(OrderBadRequestException.class)
    public ResponseEntity<ApiResponse<Void>> handleBadRequest(OrderBadRequestException ex) {
        return buildErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), null, false, ex);
    }

    // --- Validation Handlers ---

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException ex) {
        var fieldError = ex.getBindingResult().getFieldError();
        String field = fieldError != null ? fieldError.getField() : null;
        String message = fieldError != null ? fieldError.getDefaultMessage() : "Validation failed";
        return buildErrorResponse(HttpStatus.BAD_REQUEST, message, field, false, ex);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraint(ConstraintViolationException ex) {
        return buildErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), null, false, ex);
    }

    // --- Security Handlers ---

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleForbidden(AccessDeniedException ex) {
        return buildErrorResponse(HttpStatus.FORBIDDEN, "Insufficient permissions for this order action", null, false, ex);
    }

    // --- Infrastructure Handlers ---

    @ExceptionHandler(MessagingException.class)
    public ResponseEntity<ApiResponse<Void>> handleMessaging(MessagingException ex) {
        return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to publish order event to Kafka", null, true, ex);
    }

    // --- Catch-All ---

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneric(Exception ex) {
        return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred in Order Service", null, true, ex);
    }
}