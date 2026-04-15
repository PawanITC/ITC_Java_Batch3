package com.itc.funkart.gateway.exception;

import com.itc.funkart.gateway.response.ApiResponse;
import com.itc.funkart.gateway.response.ErrorDetails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private ResponseEntity<ApiResponse<Void>> buildErrorResponse(HttpStatus status, String message, String field, Exception ex) {
        // Log stack trace only for 500 errors
        if (status.is5xxServerError()) {
            log.error("Internal Server Error: ", ex);
        } else {
            log.warn("{}: {}", status.value(), message);
        }

        ErrorDetails errorDetails = new ErrorDetails(status.name(), message, field);
        return ResponseEntity.status(status).body(new ApiResponse<>(errorDetails));
    }

    /**
     * Handles validation errors in Reactive WebFlux (WebExchangeBindException).
     */
    @ExceptionHandler(WebExchangeBindException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(WebExchangeBindException ex) {
        var fieldError = ex.getBindingResult().getFieldError();
        String field = (fieldError != null) ? fieldError.getField() : "unknown";
        String message = (fieldError != null) ? fieldError.getDefaultMessage() : "Validation failed";

        return buildErrorResponse(HttpStatus.BAD_REQUEST, message, field, ex);
    }

    @ExceptionHandler(OAuthException.class)
    public ResponseEntity<ApiResponse<Void>> handleOAuth(OAuthException ex) {
        return buildErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), null, ex);
    }

    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(Exception ex) {
        return buildErrorResponse(HttpStatus.FORBIDDEN, "Access denied", null, ex);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneric(Exception ex) {
        return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred", null, ex);
    }
}