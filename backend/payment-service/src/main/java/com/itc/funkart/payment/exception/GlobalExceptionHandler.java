package com.itc.funkart.payment.exception;

import com.itc.funkart.common.dto.response.ApiResponse;
import com.stripe.exception.StripeException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * <h2>Payment Service Exception Shield</h2>
 * <p>
 * Standardizes all Payment-related failures into the global {@link ApiResponse} format.
 * Inspired by the Order Service architecture for cross-domain consistency.
 * </p>
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * Standardized internal builder to map exceptions to the ApiResponse envelope.
     */
    private ResponseEntity<ApiResponse<Void>> buildErrorResponse(
            HttpStatus status,
            String code,
            String message,
            String field,
            boolean isCritical,
            Exception ex) {

        if (isCritical) {
            log.error("✗ Payment Service Critical [{}]: {} | Code: {}", status.value(), message, code, ex);
        } else {
            log.warn("✗ Payment Service Warning [{}]: {} | Field: {}", status.value(), message, field);
        }

        return ResponseEntity.status(status)
                .body(ApiResponse.error(code, message, field));
    }

    // --- Resilience4j (Stripe API Protection) ---

    @ExceptionHandler(RequestNotPermitted.class)
    public ResponseEntity<ApiResponse<Void>> handleRateLimit(RequestNotPermitted ex) {
        return buildErrorResponse(HttpStatus.TOO_MANY_REQUESTS, "RATE_LIMIT_EXCEEDED",
                "Payment gateway rate limit reached", null, false, ex);
    }

    @ExceptionHandler(CallNotPermittedException.class)
    public ResponseEntity<ApiResponse<Void>> handleCircuitBreaker(CallNotPermittedException ex) {
        return buildErrorResponse(HttpStatus.SERVICE_UNAVAILABLE, "CIRCUIT_OPEN",
                "Stripe integration is temporarily unavailable", null, true, ex);
    }

    // --- Stripe & Payment Domain Handlers ---

    @ExceptionHandler(PaymentException.class)
    public ResponseEntity<ApiResponse<Void>> handlePaymentException(PaymentException ex) {
        return buildErrorResponse(HttpStatus.BAD_REQUEST, ex.getErrorCode(),
                ex.getMessage(), null, false, ex);
    }

    @ExceptionHandler(StripeException.class)
    public ResponseEntity<ApiResponse<Void>> handleStripeException(StripeException ex) {
        // Stripe errors often involve card declines (402 Payment Required)
        return buildErrorResponse(HttpStatus.PAYMENT_REQUIRED, "STRIPE_API_FAILURE",
                ex.getMessage(), null, true, ex);
    }

    @ExceptionHandler(WebhookException.class)
    public ResponseEntity<ApiResponse<Void>> handleWebhookException(WebhookException ex) {
        return buildErrorResponse(HttpStatus.BAD_REQUEST, "WEBHOOK_VERIFICATION_FAILED",
                ex.getMessage(), null, true, ex);
    }

    // --- Validation & Security ---

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException ex) {
        var fieldError = ex.getBindingResult().getFieldError();
        String field = fieldError != null ? fieldError.getField() : "request";
        String message = fieldError != null ? fieldError.getDefaultMessage() : "Invalid payment input";

        return buildErrorResponse(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR",
                message, field, false, ex);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleForbidden(AccessDeniedException ex) {
        return buildErrorResponse(HttpStatus.FORBIDDEN, "INSUFFICIENT_PERMISSIONS",
                "You are not authorized to perform this payment action", null, false, ex);
    }

    // --- Catch-All ---

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneric(Exception ex) {
        return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR",
                "An unexpected system failure occurred in Payment Service", null, true, ex);
    }
}