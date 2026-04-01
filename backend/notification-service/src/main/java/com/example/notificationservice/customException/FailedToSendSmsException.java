package com.example.notificationservice.customException;

public class FailedToSendSmsException extends RuntimeException {
    public FailedToSendSmsException(String message) {
        super(message);
    }
}
