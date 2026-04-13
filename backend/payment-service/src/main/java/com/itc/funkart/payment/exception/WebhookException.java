package com.itc.funkart.payment.exception;

public class WebhookException extends RuntimeException {
    public WebhookException(String message) {
        super(message);
    }
}
