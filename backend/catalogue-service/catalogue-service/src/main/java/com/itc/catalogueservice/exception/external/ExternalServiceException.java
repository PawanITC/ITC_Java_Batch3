package com.itc.catalogueservice.exception.external;


public class ExternalServiceException extends RuntimeException {
    public ExternalServiceException(String message) {
        super(message);
    }
}