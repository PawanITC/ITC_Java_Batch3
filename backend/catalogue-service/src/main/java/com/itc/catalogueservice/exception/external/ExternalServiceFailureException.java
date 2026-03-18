package com.itc.catalogueservice.exception.external;

public class ExternalServiceFailureException extends ExternalServiceException {
    public ExternalServiceFailureException() {
        super("External service failed");
    }
}