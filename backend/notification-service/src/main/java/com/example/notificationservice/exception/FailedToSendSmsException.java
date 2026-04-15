package com.example.notificationservice.exception;

public class FailedToSendSmsException extends RuntimeException {
    public FailedToSendSmsException(String message) {
        super(message);
    }
}
