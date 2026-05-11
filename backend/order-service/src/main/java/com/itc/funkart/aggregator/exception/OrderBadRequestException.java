package com.itc.funkart.aggregator.exception;

public class OrderBadRequestException extends RuntimeException {
    public OrderBadRequestException(String message) {
        super(message);
    }
}
