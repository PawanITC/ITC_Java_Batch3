package com.itc.funkart.payment.exception;

/**
 * <h2>WebhookException</h2>
 * <p>
 * Thrown when an incoming Stripe Webhook fails validation or processing.
 * </p>
 * <p>
 * Usually indicates a signature mismatch, a missing webhook secret,
 * or an unhandled Stripe event type.
 * </p>
 */
public class WebhookException extends RuntimeException {
    public WebhookException(String message) {
        super(message);
    }

    public WebhookException(String message, Throwable cause) {
        super(message, cause);
    }
}