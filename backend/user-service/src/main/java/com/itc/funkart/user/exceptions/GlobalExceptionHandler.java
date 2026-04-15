package com.itc.funkart.user.exceptions;

import com.itc.funkart.user.response.ApiResponse;
import com.itc.funkart.user.response.ErrorDetails;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * Centralized exception handling component for the User Service.
 * Intercepts exceptions thrown by controllers and transforms them into standardized
 * {@link ApiResponse} objects.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Helper method to construct a consistent error response.
     * * @param status       The HTTP status to return.
     * @param message      The human-readable error message.
     * @param logStackTrace Whether to log the full stack trace (useful for 5xx errors).
     * @param ex           The intercepted exception.
     * @return A formatted {@link ResponseEntity} containing an {@link ApiResponse}.
     */
    private ResponseEntity<ApiResponse<Void>> buildErrorResponse(HttpStatus status, String message, boolean logStackTrace, Exception ex) {
        if (logStackTrace) {
            log.error("Unhandled exception: {}", ex.getMessage(), ex);
        } else {
            log.warn("{}: {}", status.name(), message);
        }

        ErrorDetails errorDetails = new ErrorDetails(status.name(), message);
        return ResponseEntity.status(status)
                .body(new ApiResponse<>(errorDetails));
    }

    /** Handles business logic violations resulting in a 400 Bad Request. */
    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ApiResponse<Void>> handleBadRequest(BadRequestException ex) {
        return buildErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), false, ex);
    }

    /** Handles authentication failures. */
    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnauthorized(UnauthorizedException ex) {
        return buildErrorResponse(HttpStatus.UNAUTHORIZED, ex.getMessage(), false, ex);
    }

    /** Handles missing resource lookups. */
    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFound(NotFoundException ex) {
        return buildErrorResponse(HttpStatus.NOT_FOUND, ex.getMessage(), false, ex);
    }

    /** Handles data integrity violations like duplicate emails. */
    @ExceptionHandler(AlreadyExistsException.class)
    public ResponseEntity<ApiResponse<Void>> handleConflict(AlreadyExistsException ex) {
        return buildErrorResponse(HttpStatus.CONFLICT, ex.getMessage(), false, ex);
    }

    /** Handles invalid arguments passed to methods. */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgument(IllegalArgumentException ex) {
        return buildErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), false, ex);
    }

    /** Handles permission-based failures. */
    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ApiResponse<Void>> handleForbidden(ForbiddenException ex) {
        return buildErrorResponse(HttpStatus.FORBIDDEN, ex.getMessage(), false, ex);
    }

    /**
     * Handles JSR-303 validation errors for {@code @RequestBody} parameters.
     * Concatenates all field errors into a single string for client clarity.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .collect(Collectors.joining(", "));
        return buildErrorResponse(HttpStatus.BAD_REQUEST, message, false, ex);
    }

    /** Handles JSR-303 validation errors for {@code @RequestParam} or {@code @PathVariable}. */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolation(ConstraintViolationException ex) {
        return buildErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), false, ex);
    }

    /** Handles failures during JWT parsing or validation. */
    @ExceptionHandler(JwtAuthenticationException.class)
    public ResponseEntity<ApiResponse<Void>> handleJwtAuthentication(JwtAuthenticationException ex) {
        return buildErrorResponse(HttpStatus.UNAUTHORIZED, ex.getMessage(), false, ex);
    }

    /** Handles failures during the external OAuth2 handshake (e.g., GitHub). */
    @ExceptionHandler(OAuthException.class)
    public ResponseEntity<ApiResponse<Void>> handleOAuth(OAuthException ex) {
        return buildErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), true, ex);
    }

    /** Handles Security-level access denials. */
    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(Exception ex) {
        return buildErrorResponse(HttpStatus.FORBIDDEN, "Access denied", false, ex);
    }

    /** Fallback handler for all uncaught exceptions. Returns a 500 Internal Server Error. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneric(Exception ex) {
        return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error", true, ex);
    }
}