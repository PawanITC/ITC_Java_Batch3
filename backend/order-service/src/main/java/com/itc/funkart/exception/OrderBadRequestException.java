package com.itc.funkart.exception;

public class OrderBadRequestException extends RuntimeException {
    public OrderBadRequestException(String message) {
        super(message);
    }
}
