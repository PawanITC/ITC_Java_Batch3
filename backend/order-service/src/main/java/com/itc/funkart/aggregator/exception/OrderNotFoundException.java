package com.itc.funkart.aggregator.exception;

public class OrderNotFoundException extends RuntimeException {
    public OrderNotFoundException(String message) {
        super(message);
    }

    // Recommended: Dedicated constructor for ID-based lookups
    public OrderNotFoundException(Long id) {
        super(String.format("Order with ID %d not found", id));
    }
}
