package com.itc.funkart.payment.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * <h2>PaymentException</h2>
 * <p>
 * Thrown when a business rule in the payment domain is violated.
 * </p>
 * <b>Examples:</b>
 * <ul>
 * <li>Attempting to refund an already refunded payment.</li>
 * <li>Processing a payment for a non-existent order.</li>
 * </ul>
 */
@Getter
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class PaymentException extends RuntimeException {

    private final String errorCode;

    /**
     * @param message Human-readable detail.
     */
    public PaymentException(String message) {
        super(message);
        this.errorCode = "PAYMENT_PROCESSING_ERROR";
    }

    /**
     * @param message   Human-readable detail.
     * @param errorCode Machine-readable code for frontend/logic mapping.
     */
    public PaymentException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }
}