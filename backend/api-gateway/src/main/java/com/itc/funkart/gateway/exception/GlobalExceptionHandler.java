package com.itc.funkart.gateway.exception;

import com.itc.funkart.gateway.response.ApiResponse;
import com.itc.funkart.gateway.response.ErrorDetails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

/**
 * <h2>Global Exception Handler</h2>
 * <p>
 * Centralized error handling for the API Gateway. Ensures that all exceptions,
 * whether originating in the Gateway or propagated from downstream microservices,
 * are returned in the unified {@link ApiResponse} format.
 * </p>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Internal helper to build consistent error envelopes.
     *
     * @param status  The HTTP status to return.
     * @param message Human-readable error message.
     * @param field   The specific request field that failed (optional).
     * @param ex      The caught exception for logging.
     * @return A formatted ResponseEntity containing the ApiResponse.
     */
    private ResponseEntity<ApiResponse<Void>> buildErrorResponse(
            HttpStatus status,
            String code,
            String message,
            String field,
            Exception ex
    ) {
        if (status.is5xxServerError()) {
            log.error("Gateway Internal Error", ex);
        } else {
            log.warn("Request rejected [{}]: {}", status.value(), message);
        }

        ErrorDetails error = new ErrorDetails(code, message, field);
        return ResponseEntity.status(status).body(new ApiResponse<>(error));
    }

    /**
     * Handles validation errors (e.g., @Valid or @Validated failures).
     */
    @ExceptionHandler(WebExchangeBindException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(WebExchangeBindException ex) {

        var fieldError = ex.getBindingResult().getFieldError();

        String field = (fieldError != null) ? fieldError.getField() : "unknown";
        String message = (fieldError != null) ? fieldError.getDefaultMessage() : "Validation failed";

        return buildErrorResponse(
                HttpStatus.BAD_REQUEST,
                "VALIDATION_ERROR",
                message,
                field,
                ex
        );
    }

    /**
     * Handles security and JWT-specific failures.
     */
    @ExceptionHandler(OAuthException.class)
    public ResponseEntity<ApiResponse<Void>> handleOAuth(OAuthException ex) {
        return buildErrorResponse(
                HttpStatus.UNAUTHORIZED,
                "OAUTH_ERROR",
                ex.getMessage(),
                null,
                ex
        );
    }

    /**
     * Propagates errors from downstream services (User, Payment, Order) back to the client.
     * Captures the status code from the microservice but maintains the Gateway's response format.
     */
    @ExceptionHandler(WebClientResponseException.class)
    public ResponseEntity<ApiResponse<Void>> handleWebClientError(WebClientResponseException ex) {

        log.warn("Downstream Service failure: {} - {}", ex.getStatusCode(), ex.getStatusText());

        return buildErrorResponse(
                HttpStatus.valueOf(ex.getStatusCode().value()),
                "DOWNSTREAM_ERROR_" + ex.getStatusCode().value(),
                "Service Error: " + ex.getStatusText(),
                null,
                ex
        );
    }

    /**
     * Handles Spring Security access denials (403).
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(AccessDeniedException ex) {
        return buildErrorResponse(
                HttpStatus.FORBIDDEN,
                "ACCESS_DENIED",
                "Access denied: Insufficient permissions",
                null,
                ex
        );
    }

    /**
     * Catch-all handler for any unhandled runtime exceptions (500).
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneric(Exception ex) {
        return buildErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "INTERNAL_ERROR",
                "An unexpected system error occurred",
                null,
                ex
        );
    }
}