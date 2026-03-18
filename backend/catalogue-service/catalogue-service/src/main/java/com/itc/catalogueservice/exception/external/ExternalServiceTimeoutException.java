package com.itc.catalogueservice.exception.external;

public class ExternalServiceTimeoutException extends ExternalServiceException {
    public ExternalServiceTimeoutException() {
        super("External service timed out");
    }
}