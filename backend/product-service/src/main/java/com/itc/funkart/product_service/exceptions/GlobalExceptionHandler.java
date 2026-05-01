package com.itc.funkart.product_service.exceptions;

import com.itc.funkart.product_service.response.ApiResponse;
import com.itc.funkart.product_service.response.ErrorDetails;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Centralized exception handling component for the Product Service.
 * Intercepts exceptions thrown by controllers and transforms them into standardized
 * {@link ApiResponse} objects to maintain consistency across the Funkart ecosystem.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Helper method to construct a consistent error response.
     *
     * @param status        The HTTP status to return.
     * @param message       The human-readable error message.
     * @param field         The specific field related to the error (if applicable).
     * @param logStackTrace Whether to log the full stack trace (useful for 5xx errors).
     * @param ex            The intercepted exception.
     * @return A formatted {@link ResponseEntity} containing an {@link ApiResponse}.
     */
    private ResponseEntity<ApiResponse<Void>> buildErrorResponse(
            HttpStatus status,
            String message,
            String field,
            boolean logStackTrace,
            Exception ex) {

        if (logStackTrace) {
            log.error("Unhandled exception in Product Service: {}", ex.getMessage(), ex);
        } else {
            log.warn("Product Service {} error: {}", status.value(), message);
        }

        ErrorDetails errorDetails = new ErrorDetails(status.name(), message, field);
        return ResponseEntity.status(status)
                .body(new ApiResponse<>(errorDetails));
    }

    /**
     * Handles JSR-303 validation errors for {@code @RequestBody} parameters.
     * Useful for catch errors during Product creation or updates.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException ex) {
        var fieldError = ex.getBindingResult().getFieldError();
        String field = fieldError != null ? fieldError.getField() : null;
        String message = fieldError != null ? fieldError.getDefaultMessage() : "Validation failed";

        // HARDCODE the internal code to "VALIDATION_ERROR" to satisfy the test
        // and provide specific context, while keeping the status as 400.
        ErrorDetails errorDetails = new ErrorDetails("VALIDATION_ERROR", message, field);

        log.warn("Validation failed for field {}: {}", field, message);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse<>(errorDetails));
    }

    /**
     * Handles missing products or categories.
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFound(ResourceNotFoundException ex) {
        return buildErrorResponse(HttpStatus.NOT_FOUND, ex.getMessage(), null, false, ex);
    }

    /**
     * Handles business logic violations like invalid prices or stock levels.
     */
    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ApiResponse<Void>> handleBadRequest(BadRequestException ex) {
        return buildErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), null, false, ex);
    }

    /**
     * Handles data integrity violations (e.g., trying to create a product with a duplicate SKU).
     */
    @ExceptionHandler(AlreadyExistsException.class)
    public ResponseEntity<ApiResponse<Void>> handleConflict(AlreadyExistsException ex) {
        return buildErrorResponse(HttpStatus.CONFLICT, ex.getMessage(), "sku", false, ex);
    }

    /**
     * Handles security/authentication failures.
     */
    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnauthorized(UnauthorizedException ex) {
        return buildErrorResponse(HttpStatus.UNAUTHORIZED, ex.getMessage(), null, false, ex);
    }

    /**
     * Handles JSR-303 validation errors for {@code @RequestParam} or {@code @PathVariable}.
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolation(ConstraintViolationException ex) {
        return buildErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), null, false, ex);
    }

    @ExceptionHandler(EmptyCartException.class)
    public ResponseEntity<ApiResponse<Void>> handleEmptyCart(EmptyCartException ex) {
        // We use ex.getCode() for the internal API code, but still return 400 Bad Request
        ErrorDetails errorDetails = new ErrorDetails(ex.getCode(), ex.getMessage());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse<>(errorDetails));
    }

    /**
     * Handles illegal state transitions, such as checking out an empty cart.
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalState(IllegalStateException ex) {
        return buildErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), null, false, ex);
    }

    /**
     * Handles invalid arguments passed to product utility methods.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgument(IllegalArgumentException ex) {
        return buildErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), null, false, ex);
    }

    /**
     * Fallback handler for all uncaught exceptions. Returns a 500 Internal Server Error.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneric(Exception ex) {
        return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected internal server error occurred in the Product Service", null, true, ex);
    }
}