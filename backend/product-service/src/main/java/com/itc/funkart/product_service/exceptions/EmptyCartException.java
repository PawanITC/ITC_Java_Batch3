package com.itc.funkart.product_service.exceptions;

import lombok.Getter;

@Getter
public class EmptyCartException extends RuntimeException {
    private final String code;

    public EmptyCartException(String message) {
        super(message);
        this.code = "EMPTY_CART"; // Default machine-readable code
    }

    public EmptyCartException(String code, String message) {
        super(message);
        this.code = code;
    }
}