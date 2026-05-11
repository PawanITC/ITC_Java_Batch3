package com.itc.funkart.notification.exception;

public class FailedToSendSmsException extends RuntimeException {
    public FailedToSendSmsException(String message) {
        super(message);
    }
}
