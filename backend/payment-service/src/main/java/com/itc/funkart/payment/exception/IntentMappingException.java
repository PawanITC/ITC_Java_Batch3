package com.itc.funkart.payment.exception;

/**
 * <h2>IntentMappingException</h2>
 * <p>
 * Thrown when the service fails to map a Stripe {@code PaymentIntent}
 * or {@code Charge} object to internal FunKart DTOs.
 * </p>
 */
public class IntentMappingException extends RuntimeException {

    public IntentMappingException(String message) {
        super(message);
    }

    /**
     * @param message Human-readable error.
     * @param cause   The underlying deserialization exception.
     */
    public IntentMappingException(String message, Throwable cause) {
        super(message, cause);
    }
}