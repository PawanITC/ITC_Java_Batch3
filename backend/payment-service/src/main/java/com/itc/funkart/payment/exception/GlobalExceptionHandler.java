package com.itc.funkart.payment.exception;

import com.itc.funkart.payment.response.ApiResponse;
import com.itc.funkart.payment.response.ErrorDetails;
import com.stripe.exception.StripeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * <h2>GlobalExceptionHandler</h2>
 * <p>
 * The central "Safety Net" and response-transformer for the Payment Service.
 * </p>
 * <p>
 * This class interceptor catches exceptions across all layers (Controller, Service, Repository)
 * and ensures that the client always receives a standardized {@link ApiResponse} instead of
 * leaking internal system details or raw stack traces.
 * </p>
 *
 * @author Abbas
 * @version 1.3
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Handles business logic violations within the Payment domain.
     * <p>
     * Utilizes the custom {@code errorCode} defined within the {@link PaymentException}
     * to provide the frontend with machine-readable error categories.
     * </p>
     *
     * @param ex The caught PaymentException.
     * @return 400 Bad Request with domain-specific error details.
     */
    @ExceptionHandler(PaymentException.class)
    public ResponseEntity<ApiResponse<?>> handlePaymentException(PaymentException ex) {
        logger.error("✗ Payment Domain Error [{}]: {}", ex.getErrorCode(), ex.getMessage());
        return buildErrorResponse(ex.getErrorCode(), ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

    /**
     * Handles failures related to external Stripe Webhook events.
     * <p>
     * Covers signature verification failures or processing errors for asynchronous notifications.
     * </p>
     *
     * @param ex The caught WebhookException.
     * @return 400 Bad Request with webhook error context.
     */
    @ExceptionHandler(WebhookException.class)
    public ResponseEntity<ApiResponse<?>> handleWebhookException(WebhookException ex) {
        logger.error("✗ Webhook Processing Failure: {}", ex.getMessage());
        return buildErrorResponse("WEBHOOK_ERROR", ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

    /**
     * Handles exceptions originating directly from the Stripe Java SDK.
     * <p>
     * This typically includes card declines, expired tokens, or API connection issues.
     * Mapped to 402 (Payment Required) to accurately reflect the HTTP status for transaction failures.
     * </p>
     *
     * @param ex The raw StripeException from the SDK.
     * @return 402 Payment Required with Stripe's descriptive message.
     */
    @ExceptionHandler(StripeException.class)
    public ResponseEntity<ApiResponse<?>> handleStripeException(StripeException ex) {
        logger.error("✗ Stripe SDK Error: {} | Stripe-Code: {}", ex.getMessage(), ex.getCode());
        return buildErrorResponse("STRIPE_API_ERROR", ex.getMessage(), HttpStatus.PAYMENT_REQUIRED);
    }

    /**
     * Handles failures in mapping Stripe JSON payloads to internal DTOs.
     * <p>
     * Often occurs when Stripe introduces new API versions or when webhook data is malformed.
     * </p>
     *
     * @param ex The caught IntentMappingException.
     * @return 400 Bad Request.
     */
    @ExceptionHandler(IntentMappingException.class)
    public ResponseEntity<ApiResponse<?>> handleIntentMappingException(IntentMappingException ex) {
        logger.error("✗ Payload Mapping Failure: {}", ex.getMessage());
        return buildErrorResponse("MAPPING_FAILED", "Failed to process transaction metadata", HttpStatus.BAD_REQUEST);
    }

    /**
     * Handles invalid inputs, missing required fields, or failed business validations.
     *
     * @param ex The IllegalArgumentException.
     * @return 400 Bad Request with the specific validation message.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<?>> handleIllegalArgumentException(IllegalArgumentException ex) {
        logger.warn("✗ Validation Warning: {}", ex.getMessage());
        return buildErrorResponse("INVALID_ARGUMENT", ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

    /**
     * Final catch-all for any unhandled or unexpected RuntimeExceptions.
     * <p>
     * <b>Security Note:</b> Unlike domain exceptions, we do NOT leak the specific exception
     * message here to prevent exposing internal stack details to potential attackers.
     * </p>
     *
     * @param ex The generic Exception.
     * @return 500 Internal Server Error with a sanitized message.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<?>> handleGenericException(Exception ex) {
        // Log the full stack trace for internal troubleshooting
        logger.error("🚨 CRITICAL SYSTEM FAILURE: ", ex);

        return buildErrorResponse(
                "INTERNAL_SERVER_ERROR",
                "An unexpected system error occurred. Our engineering team has been notified.",
                HttpStatus.INTERNAL_SERVER_ERROR
        );
    }

    /**
     * Centralized utility to construct a standardized Error Response.
     *
     * @param code    Machine-readable error code (e.g., PAYMENT_ERROR).
     * @param message Human-readable error description.
     * @param status  The HTTP status code to return.
     * @return A wrapped {@link ResponseEntity} containing the {@link ApiResponse}.
     */
    private ResponseEntity<ApiResponse<?>> buildErrorResponse(String code, String message, HttpStatus status) {
        ErrorDetails error = new ErrorDetails(code, message);
        return new ResponseEntity<>(new ApiResponse<>(error), status);
    }
}