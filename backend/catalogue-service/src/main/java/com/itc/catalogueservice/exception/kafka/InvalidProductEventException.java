package com.itc.catalogueservice.exception.kafka;

public class InvalidProductEventException extends RuntimeException {

    public InvalidProductEventException(String message) {
        super(message);
    }
}